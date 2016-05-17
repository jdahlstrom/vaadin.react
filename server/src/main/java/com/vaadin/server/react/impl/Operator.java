package com.vaadin.server.react.impl;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import com.vaadin.server.react.Flow;
import com.vaadin.server.react.Flow.Subscriber;

/**
 * Transforms a {@link Subscriber} into another. Given a subscriber, returns a
 * new subscriber that passes the sequence of signals it receives to the
 * original subscriber, but manipulated somehow. Depending on the operator, this
 * manipulation may entail discarding values, synthetizing new values,
 * transforming the values or more.
 * 
 * @author johannesd@vaadin.com
 *
 * @param <T>
 * @param <U>
 */
public interface Operator<T, U> extends
        Function<Subscriber<? super U>, Subscriber<T>> {

    /**
     * Returns an operator that transforms a subscriber into one that passes
     * each received value through a mapping function.
     * 
     * @param mapper
     *            the mapping function
     * @return a mapping operator
     */
    public static <T, U> Operator<T, U> map(
            Function<? super T, ? extends U> mapper) {

        return to -> new Sub<T, U>(to) {
            @Override
            public void onNext(T value) {
                to.onNext(mapper.apply(value));
            }
        };
    }

    /**
     * Returns an operator that transforms a subscriber into one that filters
     * out some subset of the received values based on a predicate.
     * 
     * @param predicate
     *            the predicate used to test the values
     * @return a filtering operator
     */
    public static <T> Operator<T, T> filter(Predicate<? super T> predicate) {

        return to -> new Sub<T, T>(to) {
            @Override
            public void onNext(T value) {
                if (predicate.test(value)) {
                    to.onNext(value);
                }
            }
        };
    }

    /**
     * Returns an operator that transforms a subscriber into one that reduces
     * the values it receives into a single value.
     * 
     * @param reducer
     *            the reducing function
     * @param initial
     *            the initial value
     * @return a reduction operator
     */
    public static <T, U> Operator<T, U> reduce(BiFunction<U, T, U> reducer,
            U initial) {

        return to -> new Sub<T, U>(to) {
            private U accum = initial;

            @Override
            public void onNext(T value) {
                accum = reducer.apply(accum, value);
            }

            @Override
            public void onEnd() {
                to.onNext(accum);
                super.onEnd();
            }
        };
    }

    /**
     * Returns an operator that transforms a subscriber into one that maps
     * values into flows and concatenates the resulting sequence of flows.
     * 
     * @param mapper
     *            the mapping function
     * @return a map-and-flatten operator
     */
    public static <T, U> Operator<T, U> flatMap(
            Function<? super T, Flow<? extends U>> mapper) {
        return to -> new Sub<T, U>(to) {
            @Override
            public void onNext(T value) {
                mapper.apply(value).subscribe( //
                        to::onNext, //
                        to::onError, //
                        () -> {
                        });
            }
        };
    }

    /**
     * Returns an operator that transforms a subscriber into one that only
     * accepts an initial subsequence of values satisfying the given predicate.
     * 
     * @param predicate
     *            the predicate used to pick the prefix sequence
     * @return a prefix sequence operator
     */
    public static <T> Operator<T, T> takeWhile(Predicate<? super T> predicate) {
        return to -> new Sub<T, T>(to) {
            private boolean taking = true;

            @Override
            public void onNext(T value) {
                if (!taking) {
                    return;
                } else if (predicate.test(value)) {
                    to.onNext(value);
                } else {
                    to.onEnd();
                    taking = false;
                }
            }

            @Override
            public void onEnd() {
                if (taking) {
                    to.onEnd();
                }
            }
        };
    }

    /**
     * Returns an operator that transforms a subscriber into one that drops an
     * initial subsequence of values satisfying the given predicate.
     * 
     * @param predicate
     *            the predicate used to drop the prefix sequence
     * @return a prefix-dropping operator
     */
    public static <T> Operator<T, T> dropWhile(Predicate<? super T> predicate) {
        return to -> new Sub<T, T>(to) {
            private boolean dropping = true;

            @Override
            public void onNext(T value) {
                if (dropping) {
                    dropping = predicate.test(value);
                }
                if (!dropping) {
                    to.onNext(value);
                }
            }
        };
    }
}

/**
 * A helper subscriber wrapping another subscriber.
 * 
 * TODO consider moving elsewhere.
 * 
 * @author johannesd@vaadin.com
 *
 * @param <T>
 *            the value type of this subscriber
 * @param <U>
 *            the value type of the wrapped subscriber
 */
abstract class Sub<T, U> implements Subscriber<T> {
    private Subscriber<? super U> wrapped;

    Sub(Subscriber<? super U> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void onError(Exception e) {
        wrapped.onError(e);
    }

    @Override
    public void onEnd() {
        wrapped.onEnd();
    }
}
