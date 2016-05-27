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

import java.util.function.Consumer;

import com.vaadin.server.react.Flow;
import com.vaadin.server.react.Subscriber;

/**
 * A basic concrete implementation of a Flow.
 * 
 * @author johannesd@vaadin.com
 *
 * @param <T>
 *            the value type of this flow
 */
public class FlowImpl<T> implements Flow<T> {

    protected Consumer<Subscriber<? super T>> onSubscribe;

    protected FlowImpl() {
    }

    /**
     * Creates a new flow that invokes the given callback for each subscription
     * to the flow.
     * 
     * @param onSubscribe
     *            the subscription callback
     */
    public FlowImpl(Consumer<Subscriber<? super T>> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }

    @Override
    public Subscription subscribe(Subscriber<? super T> subscriber) {
        assert !subscriber.isSubscribed() : "subscriber cannot be already subscribed";

        Subscription sub = new Subscription();
        subscriber.onSubscribe(sub);
        onSubscribe.accept(subscriber);
        return sub;
    }

    @Override
    public <U> Flow<U> createFlow(Consumer<Subscriber<? super U>> onSubscribe) {
        return new FlowImpl<U>(onSubscribe);
    }
}
