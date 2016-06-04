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

package com.vaadin.server.react.impl;

import java.io.Serializable;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.vaadin.server.react.Flow;
import com.vaadin.server.react.Subscriber;

/**
 * Transforms a {@link Subscriber} into another. Given a subscriber, returns a
 * new subscriber that passes the sequence of signals it receives to the
 * original subscriber, but manipulated somehow. Depending on the operator, this
 * manipulation may entail discarding values, synthesizing new values,
 * transforming the values or more.
 * 
 * @author johannesd@vaadin.com
 * 
 * @param <T>
 *            the value type of the output subscriber
 * @param <U>
 *            the value type of the input subscriber
 */
public interface Operator<T, U> extends
        Function<Subscriber<? super U>, Subscriber<T>>, Serializable {

    /**
     * 
     * @param <T>
     *            the value type
     * @return a merging operator
     */
    public static <T> Operator<Flow<? extends T>, T> merge() {

        return to -> new Sub<Flow<? extends T>, T>(to) {
            int flows = 0;
            boolean end = false;

            @Override
            protected void doNext(Flow<? extends T> flow) {
                flows++;
                flow.subscribe(new Sub<T, T>(to) {

                    @Override
                    protected void doNext(T value) {
                        to.onNext(value);
                    }

                    @Override
                    protected void doEnd() {
                        flows--;
                        if (flows == 0 && end) {
                            to.onEnd();
                        }
                    }
                });
            }

            @Override
            protected void doError(Exception e) {
                to.onError(e);
                end = true;
                flows = 0;
            }

            @Override
            protected void doEnd() {
                end = true;
                if (flows == 0) {
                    to.onEnd();
                }
            }
        };
    }

    /**
     * Returns an operator that transforms a subscriber into one that passes
     * each received value through a mapping function.
     * 
     * @param <T>
     *            the output value type
     * @param <U>
     *            the input value type
     * @param mapper
     *            the mapping function
     * @return a mapping operator
     */
    public static <T, U> Operator<T, U> map(
            Function<? super T, ? extends U> mapper) {

        return to -> new Sub<T, U>(to) {
            @Override
            protected void doNext(T value) {
                to.onNext(mapper.apply(value));
            }
        };
    }

    /**
     * Returns an operator that transforms a subscriber into one that filters
     * out some subset of the received values based on a predicate.
     * 
     * @param <T>
     *            the value type
     * @param predicate
     *            the predicate used to test the values
     * @return a filtering operator
     */
    public static <T> Operator<T, T> filter(Predicate<? super T> predicate) {

        return to -> new Sub<T, T>(to) {
            @Override
            protected void doNext(T value) {
                if (predicate.test(value)) {
                    to.onNext(value);
                }
            }
        };
    }

    /**
     * Returns an operator that transforms a subscriber into one that reduces
     * the values it receives into a single {@code Optional} that either
     * contains the result or is empty if no values were received.
     * 
     * @param <T>
     *            the value type
     * @param reducer
     *            the reducing function
     * @return a reduction operator
     */
    public static <T> Operator<T, Optional<T>> reduce(
            BiFunction<? super T, ? super T, T> reducer) {
        return to -> new Sub<T, Optional<T>>(to) {
            private Optional<T> accum = Optional.empty();

            @Override
            protected void doNext(T value) {
                accum = Optional.of(
                        accum.map(a -> reducer.apply(a, value)).orElse(value));
            }

            @Override
            protected void doEnd() {
                to.onNext(accum);
                if (to.isSubscribed()) {
                    to.onEnd();
                }
            }
        };
    }

    /**
     * Returns an operator that transforms a subscriber into one that reduces
     * the values it receives into a single value, given an initial value.
     * 
     * @param <T>
     *            the output value type
     * @param <U>
     *            the input value type
     * @param initial
     *            the initial value
     * @param reducer
     *            the reducing function
     * @return a reduction operator
     */
    public static <T, U> Operator<T, U> reduce(U initial,
            BiFunction<? super U, ? super T, U> reducer) {

        return to -> new Sub<T, U>(to) {
            private U accum = initial;

            @Override
            protected void doNext(T value) {
                accum = reducer.apply(accum, value);
            }

            @Override
            protected void doEnd() {
                to.onNext(accum);
                if (to.isSubscribed()) {
                    to.onEnd();
                }
            }
        };
    }

    /**
     * Returns an operator that transforms a subscriber into one that performs a
     * <i>mutable reduction</i>, using the given {@link Collector}, to the
     * values it receives, and yields the resulting value.
     * 
     * @see Collectors
     * 
     * @param <T>
     *            the output value type
     * @param <A>
     *            the intermediate type used by the collector
     * @param <U>
     *            the input value type
     * 
     * @param collector
     *            the collector used
     * @return a collector operation
     */
    public static <T, A, U> Operator<T, U> collect(
            Collector<? super T, A, U> collector) {
        return to -> new Sub<T, U>(to) {
            private A accum = collector.supplier().get();

            @Override
            protected void doNext(T value) {
                collector.accumulator().accept(accum, value);
            }

            @Override
            protected void doEnd() {
                U result = collector.finisher().apply(accum);
                to.onNext(result);
                if (to.isSubscribed()) {
                    to.onEnd();
                }
            }
        };
    }

    /**
     * Returns an operator that transforms a subscriber into one that maps
     * values into flows and concatenates the resulting sequence of flows.
     * 
     * @param <T>
     *            the output value type
     * @param <U>
     *            the input value type
     * @param mapper
     *            the mapping function
     * @return a map-and-flatten operator
     */
    public static <T, U> Operator<T, U> flatMap(
            Function<? super T, Flow<? extends U>> mapper) {
        return to -> new Sub<T, U>(to) {
            @Override
            protected void doNext(T value) {
                mapper.apply(value).subscribe(//
                        to::onNext, //
                        to::onError, //
                        () -> {
                        });
            }
        };
    }

    /**
     * Returns an operator that transforms a subscriber into one that only
     * accepts an initial subsequence of {@code n} values.
     * 
     * @param <T>
     *            the value type
     * @param n
     *            the number of values to accept
     * @return a prefix sequence operator
     */
    public static <T> Operator<T, T> take(long n) {
        return to -> new Sub<T, T>(to) {
            private long i = 0;

            @Override
            protected void doNext(T value) {
                if (i < n) {
                    to.onNext(value);
                } else if (i == n) {
                    to.onEnd();
                    unsubscribe();
                }
                i++;
            }

            @Override
            protected void doEnd() {
                if (i < n) {
                    to.onEnd();
                }
            }
        };
    }

    /**
     * Returns an operator that transforms a subscriber into one that skips an
     * initial subsequence of {@code n} values.
     * 
     * @param <T>
     *            the value type
     * @param n
     *            the number of values to skip
     * @return a prefix sequence skipping operator
     */

    public static <T> Operator<T, T> skip(long n) {
        return to -> new Sub<T, T>(to) {
            private long i = 0;

            @Override
            protected void doNext(T value) {
                if (i < n) {
                    i++;
                } else {
                    to.onNext(value);
                }
            }
        };
    }

    /**
     * Returns an operator that transforms a subscriber into one that only
     * accepts an initial subsequence of values satisfying the given predicate.
     * 
     * @param <T>
     *            the value type
     * @param predicate
     *            the predicate used to pick the prefix sequence
     * @return a prefix sequence operator
     */
    public static <T> Operator<T, T> takeWhile(Predicate<? super T> predicate) {
        return to -> new Sub<T, T>(to) {
            private boolean taking = true;

            @Override
            protected void doNext(T value) {
                assert taking;

                if (predicate.test(value)) {
                    to.onNext(value);
                } else {
                    to.onEnd();
                    taking = false;
                    unsubscribe();
                }
            }

            @Override
            protected void doEnd() {
                if (taking) {
                    to.onEnd();
                }
            }
        };
    }

    /**
     * Returns an operator that transforms a subscriber into one that skips an
     * initial subsequence of values satisfying the given predicate.
     * 
     * @param <T>
     *            the value type
     * @param predicate
     *            the predicate used to drop the prefix sequence
     * @return a prefix-dropping operator
     */
    public static <T> Operator<T, T> skipWhile(Predicate<? super T> predicate) {
        return to -> new Sub<T, T>(to) {
            private boolean dropping = true;

            @Override
            protected void doNext(T value) {
                if (dropping) {
                    dropping = predicate.test(value);
                }
                if (!dropping) {
                    to.onNext(value);
                }
            }
        };
    }

    /**
     * Returns an operator that transforms a subscriber into one that passes a
     * single boolean value to the input subscriber, indicating whether any of
     * the values it receives satisfy a predicate. The subscriber is
     * short-circuiting: the predicate is not invoked for values beyond the
     * first one to pass it.
     * 
     * @param <T>
     *            the value type
     * @param predicate
     *            the predicate to use
     * @return an existential quantifier operator
     */
    public static <T> Operator<T, Boolean> anyMatch(
            Predicate<? super T> predicate) {
        return to -> new Sub<T, Boolean>(to) {
            private boolean done = false;

            @Override
            protected void doNext(T value) {
                if (!done && predicate.test(value)) {
                    to.onNext(true);
                    if (to.isSubscribed()) {
                        to.onEnd();
                    }
                    done = true;
                    unsubscribe();
                }
            }

            @Override
            protected void doEnd() {
                if (!done) {
                    to.onNext(false);
                    if (to.isSubscribed()) {
                        to.onEnd();
                    }
                }
            };
        };
    }

    /**
     * Returns an operator that transforms a subscriber into one that passes a
     * single boolean value to the input subscriber, indicating whether every
     * value it receives satisfies a predicate. The subscriber is
     * short-circuiting: the predicate is not invoked for values beyond the
     * first one to fail it.
     * 
     * @param <T>
     *            the value type
     * @param predicate
     *            the predicate to use
     * @return a universal quantifier operator
     */
    public static <T> Operator<T, Boolean> allMatch(
            Predicate<? super T> predicate) {
        return to -> new Sub<T, Boolean>(to) {
            private boolean done = false;

            @Override
            protected void doNext(T value) {
                if (!done && !predicate.test(value)) {
                    to.onNext(false);
                    if (to.isSubscribed()) {
                        to.onEnd();
                    }
                    done = true;
                    unsubscribe();
                }
            }

            @Override
            protected void doEnd() {
                if (!done) {
                    to.onNext(true);
                    if (to.isSubscribed()) {
                        to.onEnd();
                    }
                }
            };
        };
    }
}

/**
 * A helper subscriber wrapping another subscriber.
 * 
 * TODO consider moving elsewhere and renaming.
 * 
 * @author johannesd@vaadin.com
 * 
 * @param <T>
 *            the value type of this subscriber
 * @param <U>
 *            the value type of the wrapped subscriber
 */
abstract class Sub<T, U> extends SubscriberImpl<T> {
    private Subscriber<? super U> wrapped;

    /**
     * Creates a new {@code Sub} wrapping the given subscriber.
     * 
     * @param wrapped
     *            the subscriber to be wrapped
     */
    Sub(Subscriber<? super U> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void onNext(T value) {
        assert isSubscribed();
        if (wrapped.isSubscribed()) {
            doNext(value);
        } else {
            unsubscribe();
        }
    }

    @Override
    public void onError(Exception e) {
        assert isSubscribed();
        if (wrapped.isSubscribed()) {
            doError(e);
        } else {
            unsubscribe();
        }
    }

    @Override
    public void onEnd() {
        assert isSubscribed();
        if (wrapped.isSubscribed()) {
            doEnd();
        } else {
            unsubscribe();
        }
    }

    /**
     * Invoked by {@link #onNext(Object) onNext} but only if the wrapped
     * subscriber is still subscribed.
     * 
     * @param value
     *            the next value in the flow
     */
    protected abstract void doNext(T value);

    /**
     * Invoked by {@link #onError(Exception) onError} but only if the wrapped
     * subscriber is still subscribed.
     * 
     * @param e
     *            the error
     */
    protected void doError(Exception e) {
        wrapped.onError(e);
    }

    /**
     * Invoked by {@link #onEnd() onEnd} but only if the wrapped subscriber is
     * still subscribed.
     */
    protected void doEnd() {
        wrapped.onEnd();
    }
}
