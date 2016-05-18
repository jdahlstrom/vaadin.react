/*
 * Copyright 2016 Johannes Dahlström
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.server.react;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
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
 * A flow may be asynchronous; that is, its subscribers may be invoked from a
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
public interface Flow<T> extends Serializable {

    /**
     * A subscriber to a flow.
     *
     * @param <T>
     *            the accepted value type
     */
    public interface Subscriber<T> extends Serializable {

        /**
         * Returns a new subscriber that invokes the {@code onNext} consumer for
         * each value received. Error and end signals are ignored.
         * 
         * @param <T>
         *            the value type
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
         * @param <T>
         *            the value type
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
         * Invoked in case of an error condition. This can happen at most once
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

    /**
     * A subscription token.
     *
     * TODO: Figure out how this should work
     */
    public interface Subscription extends Serializable {
        /**
         * Ends this subscription. After calling this method, the associated
         * subscriber will not receive any more notifications from the
         * respective flow.
         */
        public void unsubscribe();
    }

    /**
     * Returns a new cold flow that produces the given sequence of values and
     * then completes.
     * 
     * @param <T>
     *            the value type of the flow
     * @param values
     *            the sequence of values
     * @return a flow producing the values
     */
    @SafeVarargs
    public static <T> Flow<T> from(T... values) {
        return new FlowImpl<>(subscriber -> {
            for (T t : values) {
                subscriber.onNext(t);
            }
            subscriber.onEnd();
        });
    }

    /**
     * Returns a new cold flow that produces the given sequence of values and
     * then completes.
     * 
     * @param <T>
     *            the value type of the flow
     * @param values
     *            the sequence of values
     * @return a flow producing the values.
     */
    public static <T> Flow<T> from(Iterable<T> values) {
        return new FlowImpl<>(subscriber -> {
            for (T t : values) {
                subscriber.onNext(t);
            }
            subscriber.onEnd();
        });
    }

    /**
     * Returns a new flow drawing values from the given supplier. The supplier
     * is invoked until it returns an empty {@code Optional}; at that point the
     * flow completes. Any exception thrown by the supplier terminates the flow
     * with an error signal.
     * 
     * @param <T>
     *            the value type of the flow
     * @param supplier
     *            the producer of values
     * @return a flow yielding values from the supplier
     */
    public static <T> Flow<T> from(Supplier<Optional<T>> supplier) {
        return new FlowImpl<>(subscriber -> {
            Optional<T> opt = Optional.empty();
            Exception ex = null;
            do {
                try {
                    opt = supplier.get();
                } catch (Exception e) {
                    ex = e;
                }
                opt.ifPresent(subscriber::onNext);
            } while (ex == null && opt.isPresent());

            if (ex != null) {
                subscriber.onError(ex);
            }
            subscriber.onEnd();

        });
    }

    /**
     * Creates a new flow with the given subscription callback.
     * 
     * @param <U>
     *            the value type of the new flow
     * @param onSubscribe
     *            the callback invoked on each subscription
     * @return a new flow
     */
    public abstract <U> Flow<U> createFlow(
            Consumer<Subscriber<? super U>> onSubscribe);

    /**
     * Subscribes the given {@code Subscriber} to this flow. Its {@code onNext},
     * {@code onError}, and {@code onEnd} methods are invoked whenever this flow
     * produces a value, throws an error, or completes, respectively.
     * 
     * @param subscriber
     *            the subscriber to subscribe
     * @return the subscription
     */
    public abstract Subscription subscribe(Subscriber<? super T> subscriber);

    /**
     * Subscribes the given {@code Consumer} to this flow. The function is
     * invoked for each value produced by this flow. Termination signals are
     * discarded.
     * 
     * @param onNext
     *            the function consuming the values
     * @return the subscription
     */
    public default Subscription subscribe(Consumer<? super T> onNext) {
        return subscribe(Subscriber.from(onNext));
    }

    /**
     * Subscribes a set of three functions to this flow. The appropriate
     * function is invoked whenever this flow produces a value, throws an error,
     * or completes.
     * 
     * @param onNext
     *            the function invoked for values
     * @param onError
     *            the function invoked for exceptions
     * @param onEnd
     *            the function invoked at completion
     * @return the subscription
     */
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
     * Invokes the given consumer for each value in this flow. Error and end
     * signals are ignored.
     * 
     * @param consumer
     *            the function to invoke
     */
    public default void forEach(Consumer<? super T> consumer) {
        subscribe(consumer);
    }

    /**
     * Returns a new flow that constitutes the results of passing each value in
     * this flow through the given transformation. Error and end signals are
     * passed through as is.
     * 
     * @param <U>
     *            the output value type
     * @param mapper
     *            the function to transform the values
     * @return a flow with the transformed values
     */
    public default <U> Flow<U> map(Function<? super T, ? extends U> mapper) {
        return lift(Operator.map(mapper));
    }

    /**
     * Returns a new flow constituting the values in this flow that satisfy the
     * given predicate. Error and end signals are passed through as is.
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
     * @param <U>
     *            the output value type
     * @param reducer
     *            the reduction function
     * @param initial
     *            the initial value for reduction
     * @return a flow yielding the result of the reduction
     */
    public default <U> Flow<U> reduce(BiFunction<U, T, U> reducer, U initial) {
        return lift(Operator.reduce(reducer, initial));
    }

    /**
     * Returns a new flow constructed by invoking the given flow-returning
     * function for each value in this flow and concatenating the resulting
     * flows into a single flow. Thus, the end signals from the comprising flows
     * are not propagated to the new flow; any error, however, is.
     * <p>
     * Given some function {@code f : Function<T, Flow<U>>}, invoking
     * {@link #map(Function) map(f)} would return a flow of type
     * {@code Flow<Flow<U>>}. The {@code flatMap} method instead flattens the
     * flow of flows into a single {@code Flow<U>}.
     * 
     * @param <U>
     *            the value type of the returned flow
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
     * applied to any subsequent values. If this flow terminates before the
     * predicate fails, the returned flow terminates as well.
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
     * {@code false}. The predicate is not applied to any subsequent values. If
     * this flow terminates before the predicate fails, the returned flow
     * terminates as well without producing any values.
     * 
     * @param predicate
     *            the predicate to apply
     * @return a flow excluding the initial values that satisfy the predicate
     */
    public default Flow<T> dropWhile(Predicate<? super T> predicate) {
        return lift(Operator.dropWhile(predicate));
    }

    /**
     * Returns a flow that yields at most a single boolean value, indicating
     * whether any of the values in this flow satisfy the given predicate or
     * not. The flow is short-circuiting: the first value to pass the predicate
     * yields {@code true} and the predicate is not applied to any subsequent
     * values. The flow yields {@code false} if and only if this flow completes
     * before the predicate returns {@code true}.
     * 
     * @param predicate
     *            the predicate to apply
     * @return a flow indicating whether any values pass the predicate
     */
    public default Flow<Boolean> any(Predicate<? super T> predicate) {
        return lift(Operator.any(predicate));
    }

    /**
     * Returns a flow that yields at most a single boolean value, indicating
     * whether every value in this flow satisfies the given predicate or not.
     * The flow is short-circuiting: the first value to fail the predicate
     * yields {@code false} and the predicate is not applied to any subsequent
     * values. The flow yields {@code true} if and only if this flow completes
     * before the predicate returns {@code false}.
     * 
     * @param predicate
     *            the predicate to apply
     * @return a flow indicating whether all values pass the predicate
     */
    public default Flow<Boolean> all(Predicate<? super T> predicate) {
        return lift(Operator.all(predicate));
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
     * this flow eventually completes. If this flow terminates via an error, the
     * error is propagated and a count is not provided.
     * 
     * @return a flow with the number of values in this flow, if finite
     */
    public default Flow<Long> count() {
        return reduce((count, x) -> count + 1L, 0L);
    }

    /**
     * Returns a flow with the first {@code n} values in this flow. If this flow
     * terminates prematurely, the new flow terminates as well.
     * 
     * @param n
     *            the number of initial values to pick
     * @return a flow with the initial values
     */
    public default Flow<T> take(long n) {
        return takeWhile(new Counter(n));
    }

    /**
     * Returns a flow with all the values beyond the initial {@code n} values in
     * this flow. If this flow terminates prematurely, the new flow terminates
     * as well without producing any values.
     * 
     * @param n
     *            the number of initial values to drop
     * @return a flow without the initial values
     */
    public default Flow<T> drop(long n) {
        return dropWhile(new Counter(n));
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
     * @param <U>
     *            the value type of the new flow
     * @param op
     *            the operator to adapt subscribers
     * @return a flow with adapted subscribers
     */
    public default <U> Flow<U> lift(Operator<? super T, ? extends U> op) {
        return createFlow(s -> subscribe(op.apply(s)));
    }

    /**
     * A predicate that returns {@code true} for the first {@code n} times it is
     * invoked, and {@code false} afterwards.
     *
     */
    public class Counter implements Predicate<Object>, Serializable {
        private long count = 0;

        /**
         * @param n
         *            the number of times this counter returns {@code true}
         */
        public Counter(long n) {
            count = n;
        }

        @Override
        public boolean test(Object ignored) {
            if (count > 0) {
                count--;
                return true;
            }
            return false;
        }
    }
}
