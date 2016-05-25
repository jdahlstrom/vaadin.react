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
import java.util.function.Consumer;

import com.vaadin.server.react.Flow.Subscription;
import com.vaadin.server.react.impl.SubscriberImpl;

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
        return new SubscriberImpl<T>() {

            @Override
            public void onNext(T value) {
                onNext.accept(value);
            }
        };
    }

    /**
     * Returns a new subscriber that invokes the {@code onNext} consumer for
     * each value received; the {@code onError} consumer for any error received;
     * and the {@code onEnd} function when the flow completes.
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
        return new SubscriberImpl<T>() {

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
     * Invoked when subscribing to a flow. Storing the given subscription token
     * allows for later unsubscribing from the flow.
     * 
     * @param sub
     *            the subscription token
     */
    public void onSubscribe(Subscription sub);

    /**
     * Unsubscribes this subscriber if currently subscribed to a flow, otherwise
     * does nothing.
     * 
     * @see Subscription#unsubscribe()
     */
    public void unsubscribe();

    /**
     * @see Subscription#isSubscribed()
     * 
     * @return whether this subscriber is subscribed to a flow
     */
    public boolean isSubscribed();

    /**
     * Invoked for each value in the subscribed flow.
     * 
     * @param value
     *            the next value in the flow
     */
    public void onNext(T value);

    /**
     * Invoked in case of an error condition. This can happen at most once for a
     * given flow.
     * 
     * @param e
     *            the exception thrown
     */
    public void onError(Exception e);

    /**
     * Invoked when the flow completes. This can happen at most once for a given
     * flow, but will not occur if the flow never completes.
     */
    public void onEnd();
}
