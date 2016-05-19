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

import com.vaadin.server.react.Flow.Subscriber;
import com.vaadin.server.react.Flow.Subscription;

/**
 * 
 * @author johannesd@vaadin.com
 *
 * @param <T>
 *            the value type accepted by this subscriber
 */
public abstract class SubscriberImpl<T> implements Subscriber<T> {
    private Subscription subscription;

    @Override
    public void onSubscribe(Subscription sub) {
        subscription = sub;
    }

    @Override
    public void onError(Exception e) {
    }

    @Override
    public void onEnd() {
    }

    @Override
    public void unsubscribe() {
        if (subscription != null) {
            subscription.unsubscribe();
        }
        subscription = null;
    }

    @Override
    public boolean isSubscribed() {
        return subscription != null && subscription.isSubscribed();
    }
}
