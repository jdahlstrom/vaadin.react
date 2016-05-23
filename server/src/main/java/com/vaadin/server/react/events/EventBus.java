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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.vaadin.server.react.Flow;
import com.vaadin.server.react.Subscriber;
import com.vaadin.server.react.impl.FlowImpl;

/**
 * A hub managing event flows of different types.
 * 
 * @author johannesd@vaadin.com
 * 
 * @see Flow
 */
public class EventBus {

    private class HotFlow<T> extends FlowImpl<T> {

        private Set<Subscriber<? super T>> subscribers = new LinkedHashSet<>();

        private Consumer<Subscriber<? super T>> onSubscribe;

        HotFlow() {
            super.onSubscribe = sub -> {
                subscribers.add(sub);
                if (this.onSubscribe != null) {
                    this.onSubscribe.accept(sub);
                }
            };
        }

        void next(T value) {
            subscribers.forEach(s -> s.onNext(value));
        }
    }

    @SuppressWarnings("rawtypes")
    private Map<Class, HotFlow> flows = new LinkedHashMap<>();

    public <E extends Event> void onSubscribe(Class<E> eventType,
            Consumer<Subscriber<? super E>> onSubscribe) {
        ((HotFlow<E>) events(eventType)).onSubscribe = onSubscribe;
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
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Flow<E> flow = flows.computeIfAbsent(eventType, type -> new HotFlow());
        return flow;
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
            flow.next(event);
            return flow;
        });
    }

    public <E extends Event> void fireEvent(E event) {
        fireEvent(event.getClass(), event);
    }
}
