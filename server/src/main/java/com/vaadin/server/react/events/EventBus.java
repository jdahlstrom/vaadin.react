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

package com.vaadin.server.react.events;

import java.io.Serializable;
import java.util.EventListener;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.vaadin.server.react.Flow;
import com.vaadin.server.react.Flow.Subscription;
import com.vaadin.server.react.Subscriber;
import com.vaadin.server.react.impl.FlowImpl;

/**
 * A hub managing event flows of different types.
 *
 * @author johannesd@vaadin.com
 *
 * @see Flow
 */
public class EventBus implements Serializable {

    private class EventFlow<T> extends FlowImpl<T> {

        private Map<EventListener, Subscription> legacyListeners = new LinkedHashMap<>();

        private Set<Subscriber<? super T>> subscribers = new LinkedHashSet<>();

        private Consumer<Subscriber<? super T>> onSubscribe;

        EventFlow() {
            super.onSubscribe = sub -> {
                subscribers.add(sub);
                if (this.onSubscribe != null) {
                    this.onSubscribe.accept(sub);
                }
            };
        }

        @Override
        public Subscription subscribe(Subscriber<? super T> subscriber) {
            super.subscribe(subscriber);
            return new Subscription() {
                @Override
                public void unsubscribe() {
                    if (isSubscribed()) {
                        super.unsubscribe();
                        subscriber.unsubscribe();
                        subscribers.remove(subscriber);
                    }
                }
            };
        }

        void next(T value) {
            subscribers.forEach(s -> s.onNext(value));
        }
    }

    private Map<Class<? extends Event>, EventFlow<? extends Event>> flows = new LinkedHashMap<>();

    public <E extends Event> void onSubscribe(Class<E> eventType,
            Consumer<Subscriber<? super E>> onSubscribe) {
        eventFlow(eventType).onSubscribe = onSubscribe;
    }

    /**
     * Returns the event flow of the given type. If no such flow exist, one is
     * created.
     *
     * @param eventType
     *            the type of the events
     * @return the flow
     */
    public <E extends Event> Flow<E> events(Class<E> eventType) {
        return eventFlow(eventType);
    }

    protected <E extends Event> EventFlow<E> eventFlow(Class<E> eventType) {
        @SuppressWarnings("unchecked")
        EventFlow<E> flow = (EventFlow<E>) flows.computeIfAbsent(eventType,
                type -> new EventFlow<E>());
        return flow;
    }

    public <E extends Event, L extends EventListener & Consumer<E>> void addListener(
            Class<E> eventType, L listener) {
        addListener(eventType, listener, listener);
    }

    public <E extends Event, L extends EventListener> void addListener(
            Class<E> eventType, L listener, Consumer<? super E> method) {
        Subscription s = eventFlow(eventType).subscribe(method);
        eventFlow(eventType).legacyListeners.put(listener, s);
    }

    public <E extends Event, L extends EventListener> void removeListener(
            Class<E> eventType, L listener) {
        eventFlow(eventType).legacyListeners.computeIfPresent(listener,
                (l, s) -> {
                    s.unsubscribe();
                    return null;
                });
    }

    public Set<? extends EventListener> getListeners(
            Class<? extends Event> eventType) {
        return eventFlow(eventType).legacyListeners.keySet();
    }

    /**
     * Returns whether this event bus has any subscribers to events of the given
     * type.
     *
     * @param eventType
     *            the event class literal
     * @return {@code true} if there are subscribers, {@code false} otherwise.
     */
    public boolean hasSubscribers(Class<? extends Event> eventType) {
        return flows.containsKey(eventType)
                && !flows.get(eventType).subscribers.isEmpty();
    }

    /**
     * Pushes the given event into the flow of its respective type, if any. All
     * subscribers to that flow are notified of the event. If no such flow
     * exists, does nothing.
     *
     * @param <E>
     *            the event type
     * @param type
     *            the event Class instance
     * @param event
     *            the event to be fired
     */
    public <E extends Event> void fireEvent(Class<? extends E> type, E event) {

        flows.computeIfPresent(type, (t, flow) -> {
            ((EventFlow<E>) flow).next(event);
            return flow;
        });
    }

    public <E extends Event> void fireEvent(E event) {
        fireEvent(event.getClass(), event);
    }
}
