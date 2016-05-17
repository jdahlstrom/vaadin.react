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

import com.vaadin.server.react.Flow;
import com.vaadin.server.react.Flow.Subscriber;
import com.vaadin.server.react.impl.FlowImpl;

/**
 * A hub managing event flows of different types.
 * 
 * @author johannesd@vaadin.com
 * 
 * @see Flow
 */
public class EventBus {

    private class FlowSubscribers<E extends Event> {

        private Set<Subscriber<? super E>> subscribers = new LinkedHashSet<>();
        private Flow<E> events = new FlowImpl<>(s -> subscribers.add(s));
    }

    private Map<Class<? extends Event>, FlowSubscribers<? extends Event>> flows = new LinkedHashMap<>();

    /**
     * Returns the event flow of the given type. If no such flow exist, one is
     * created.
     * 
     * @param klass
     *            the type of the events
     * @return the flow
     */
    public <E extends Event> Flow<E> events(Class<E> klass) {
        // TODO: Get rid of unchecked cast if possible
        @SuppressWarnings("unchecked")
        Flow<E> flow = (Flow<E>) flows.computeIfAbsent(klass,
                k -> new FlowSubscribers<E>()).events;
        return flow;

    }

    /**
     * Pushes the given event into the flow of its respective type, if any. All
     * subscribers to that flow are notified of the event. If no such flow
     * exists, does nothing.
     * 
     * @param event
     */
    public <E extends Event> void fireEvent(E event) {
        flows.computeIfPresent(event.getClass(), (k, flow) -> {
            flow.subscribers.forEach(sub -> sub.onNext(event));
            return flow;
        });
    }
}
