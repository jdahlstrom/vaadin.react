package com.vaadin.server.react;

import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.vaadin.server.react.impl.FlowImpl;
import com.vaadin.server.react.impl.Operator;

/**
 * A subscribable, possibly asynchronous sequence of values.
 * <p>
 * Flows are the mathematical <i>dual</i> of {@link Stream streams} and
 * {@link Iterable iterables}. Whereas the latter constitute values that are
 * <i>pulled</i> (for instance, by means of an {@link Iterator iterator}), flows
 * instead <i>push</i> values to their {@link Subscriber subscribers}. A flow
 * thus implements inversion of control compared to streams and iterables.
 * <p>
 * A subscriber is a callback that the flow invokes with three different
 * signals. A <i>next</i> signal is simply the next value in the flow; an
 * <i>error</i> signal denotes an error condition when computing the next value;
 * and an <i>end</i> signal signifies the completion of the flow. An end signal
 * always immediately follows an error signal, and no signal may follow the end
 * signal. A flow may produce an unbounded number of values and never signal
 * completion.
 * <p>
 * Flows may be asynchronous; that is, its subscribers may be invoked from a
 * thread different from the one in which the subscription occurred; the
 * subscribe method may return before the flow completes. If the flow is
 * asynchronous, it is the responsibility of the subscriber to synchronize its
 * access to any mutable shared state.
 * <p>
 * Flows can also be <i>hot</i> or <i>cold</i>. A new subscriber to a hot flow
 * will only receive the values that are computed after the subscription. A cold
 * flow, on the other hand, provides all its values to each of its subscribers
 * regardless of the time the subscription occurred.
 * 
 * @author johannesd@vaadin.com
 *
 * @param <T>
 *            The type of the values in this flow.
 */
public interface Flow<T> {

    /**
     * A subscriber to a flow.
     *
     * @param <T>
     *            the accepted value type
     */
    public interface Subscriber<T> {

        /**
         * Returns a new subscriber that invokes the {@code onNext} consumer for
         * each value received. Error and end signals are ignored.
         * 
         * @param onNext
         *            the value consumer
         * @return a subscriber delegating to the given consumer
         */
        public static <T> Subscriber<T> from(Consumer<? super T> onNext) {
            return new Subscriber<T>() {

                @Override
                public void onNext(T value) {
                    onNext.accept(value);
                }

                @Override
                public void onError(Exception e) {
                }

                @Override
                public void onEnd() {
                }
            };
        }

        /**
         * Returns a new subscriber that invokes the {@code onNext} consumer for
         * each value received; the {@code onError} consumer for any error
         * received; and the {@code onEnd} function when the flow completes.
         * 
         * @param onNext
         *            the value consumer
         * @param onError
         *            the error consumer
         * @param onEnd
         *            the end listener
         * @return a subscriber delegating to the given functions
         */
        public static <T> Subscriber<T> from(Consumer<? super T> onNext,
                Consumer<? super Exception> onError, Runnable onEnd) {
            return new Subscriber<T>() {

                @Override
                public void onNext(T value) {
                    onNext.accept(value);
                }

                @Override
                public void onError(Exception e) {
                    onError.accept(e);

                }

                @Override
                public void onEnd() {
                    onEnd.run();
                }
            };
        }

        /**
         * Invoked for each value in the subscribed flow.
         * 
         * @param value
         *            the next value in the flow
         */
        public void onNext(T value);

        /**
         * Invoked if an error condition occurs. This can happen at most once
         * for a given flow.
         * 
         * @param e
         *            the exception thrown
         */
        public void onError(Exception e);

        /**
         * Invoked when the flow completes. This can happen at most once for a
         * given flow, but will not occur if the flow never completes.
         */
        public void onEnd();
    }

    public interface Subscription {
        public void unsubscribe();
    }

    @SafeVarargs
    public static <T> Flow<T> from(T... values) {
        return new FlowImpl<>(subscriber -> {
            for (T t : values) {
                subscriber.onNext(t);
            }
            subscriber.onEnd();
        });
    }

    public Subscription subscribe(Subscriber<? super T> subscriber);

    public default Subscription subscribe(Consumer<? super T> onNext) {
        return subscribe(Subscriber.from(onNext));
    }

    public default Subscription subscribe(Consumer<? super T> onNext,
            Consumer<? super Exception> onError, Runnable onEnd) {
        return subscribe(Subscriber.from(onNext, onError, onEnd));
    }

    /*
     * Combinators.
     * 
     * Each of these methods returns a new flow based on this flow, with some
     * transformation applied. Most of the methods are higher-order functions
     * accepting an user-provided callback to guide the transformation.
     */

    /**
     * Returns a new flow that constitutes the results of passing each value in
     * this flow through the given transformation.
     * 
     * @param mapper
     *            the function to transform the values
     * @return a flow with the transformed values
     */
    public default <U> Flow<U> map(Function<? super T, ? extends U> mapper) {
        return lift(Operator.map(mapper));
    }

    /**
     * Returns a new flow constituting the values in this flow that satisfy the
     * given predicate.
     * 
     * @param predicate
     *            the predicate to apply to the values
     * @return a flow with exactly the values for which the predicate returns
     *         true
     */
    public default Flow<T> filter(Predicate<? super T> predicate) {
        return lift(Operator.filter(predicate));
    }

    /**
     * Returns a new flow containing at most a single value: the result of
     * passing each value in this flow through the given reduction function. The
     * returned flow provides the result, and an {@link Subscriber#onEnd()
     * onEnd} signal, if and only if this flow also eventually signals
     * {@code onEnd}.
     * 
     * @param reducer
     *            the reduction function
     * @param initial
     *            the initial value for reduction
     * @return
     */
    public default <U> Flow<U> reduce(BiFunction<U, T, U> reducer, U initial) {
        return lift(Operator.reduce(reducer, initial));
    }

    /**
     * Returns a new flow constructed by invoking the given flow-returning
     * function for each value in this flow and concatenating the resulting
     * flows into a single flow.
     * <p>
     * Given some function {@code f : Function<T, Flow<U>>}, invoking
     * {@link #map(Function) map(f)} would return a flow of type
     * {@code Flow<Flow<U>>}. The {@code flatMap} method instead flattens the
     * flow of flows into a single {@code Flow<U>}.
     * 
     * @param mapper
     *            the mapping function
     * @return a flattened flow of results
     */
    public default <U> Flow<U> flatMap(
            Function<? super T, Flow<? extends U>> mapper) {
        return lift(Operator.flatMap(mapper));
    }

    /**
     * Returns a flow constituting all the values in this flow up to, but
     * excluding, the first value for which the given predicate returns
     * {@code false}. At that point, the flow completes and the predicate is not
     * applied to any subsequent values.
     * 
     * @param predicate
     *            the predicate to apply
     * @return a flow of the initial values that satisfy the predicate
     */
    public default Flow<T> takeWhile(Predicate<? super T> predicate) {
        return lift(Operator.takeWhile(predicate));
    }

    /**
     * Returns a flow constituting every value in this flow starting from,
     * inclusive, the first value for which the given predicate returns
     * {@code false}. The predicate is not applied to any subsequent values.
     * 
     * @param predicate
     *            the predicate to apply
     * @return a flow excluding the initial values that satisfy the predicate
     */
    public default Flow<T> dropWhile(Predicate<? super T> predicate) {
        return lift(Operator.dropWhile(predicate));
    }

    /*
     * Combinators implemented in terms of other combinators.
     * 
     * TODO: The default implementations of the more fundamental combinators
     * above should probably be moved to a concrete implementing class at some
     * point.
     */

    /**
     * Returns a flow with at most a single value: the number of values in this
     * flow. The returned flow provides a value, and completes, if and only if
     * this flow eventually completes.
     * 
     * @return a flow with the number of values in this flow, if finite
     */
    public default Flow<Long> count() {
        return reduce((count, x) -> count + 1L, 0L);
    }

    /**
     * Returns a flow with the first {@code n} values in this flow, or
     * {@code this.count()} values if {@code n > this.count()}.
     * 
     * @param n
     *            the number of initial values to pick
     * @return a flow with the initial values
     */
    public default Flow<T> take(long n) {
        return takeWhile(new Predicate<T>() {
            private int count = 0;

            @Override
            public boolean test(T t) {
                return count++ < n;
            }
        });
    }

    /**
     * Returns a flow with all the values beyond the initial {@code n} values in
     * this flow, or an empty flow if {@code n > this.count()}.
     * 
     * @param n
     *            the number of initial values to drop
     * @return a flow without the initial values
     */
    public default Flow<T> drop(long n) {
        return dropWhile(new Predicate<T>() {
            private int count = 0;

            @Override
            public boolean test(T t) {
                return count++ < n;
            }
        });
    }

    /*
     * Helper methods.
     * 
     * TODO: Figure out whether these should be moved elsewhere.
     */

    /**
     * Returns a new flow, possibly of different value type, whose subscribers
     * are adapted to this flow using the given operator. That is, any
     * subscribers to the new flow are passed to {@link Operator#apply(Object)
     * op.apply()} and the results subscribed to this flow.
     * 
     * @param op
     *            the operator with which to adapt subscribers
     * @return a flow with adapted subscribers
     */
    public default <U> Flow<U> lift(Operator<? super T, ? extends U> op) {
        return new FlowImpl<>(s -> subscribe(op.apply(s)));
    }
}
