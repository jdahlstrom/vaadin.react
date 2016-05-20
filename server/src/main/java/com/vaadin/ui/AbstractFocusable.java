/*
 * Copyright 2000-2014 Vaadin Ltd.
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
package com.vaadin.ui;

import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.BlurNotifier;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.event.FieldEvents.FocusNotifier;
import com.vaadin.server.react.Flow;
import com.vaadin.server.react.events.EventBus;
import com.vaadin.shared.EventId;
import com.vaadin.shared.communication.FieldRpc.FocusAndBlurServerRpc;
import com.vaadin.shared.ui.ComponentStateUtil;
import com.vaadin.shared.ui.TabIndexState;
import com.vaadin.ui.Component.Focusable;

/**
 * An abstract base class for focusable components. Includes API for setting the
 * tab index, programmatic focusing, and adding focus and blur listeners.
 * 
 * @since 7.6
 * @author Vaadin Ltd
 */
public abstract class AbstractFocusable extends AbstractComponent implements
        Focusable, FocusNotifier, BlurNotifier {

    private EventBus eventBus = new EventBus();

    protected AbstractFocusable() {
        registerRpc(new FocusAndBlurServerRpc() {

            @Override
            public void blur() {
                System.out.println("GOT BLUR RPC");
                getEventBus().fireEvent(BlurEvent.class,
                        new BlurEvent(AbstractFocusable.this));
            }

            @Override
            public void focus() {
                System.out.println("GOT FOCUS RPC");
                getEventBus().fireEvent(FocusEvent.class,
                        new FocusEvent(AbstractFocusable.this));
            }
        });

        eventBus.onSubscribe(BlurEvent.class, sub -> {
            ComponentStateUtil.addRegisteredEventListener(getState(),
                    EventId.BLUR);
        });

        eventBus.onSubscribe(FocusEvent.class, sub -> {
            ComponentStateUtil.addRegisteredEventListener(getState(),
                    EventId.FOCUS);
        });
    }

    /**
     * Returns the flow of blur events emitted by this component.
     * 
     * @return blur events
     */
    public Flow<BlurEvent> blurs() {
        return getEvents(BlurEvent.class);
    }

    @Override
    public void addBlurListener(BlurListener listener) {
        blurs().subscribe(listener::blur);
    }

    /**
     * @deprecated As of 7.0, replaced by {@link #addBlurListener(BlurListener)}
     */
    @Override
    @Deprecated
    public void addListener(BlurListener listener) {
        addBlurListener(listener);
    }

    @Override
    public void removeBlurListener(BlurListener listener) {
        throw new UnsupportedOperationException(
                "Click listeners cannot currently be removed");
    }

    /**
     * @deprecated As of 7.0, replaced by
     *             {@link #removeBlurListener(BlurListener)}
     */
    @Override
    @Deprecated
    public void removeListener(BlurListener listener) {
        removeBlurListener(listener);

    }

    /**
     * Returns the flow of focus events emitted by this component.
     * 
     * @return focus events
     */
    public Flow<FocusEvent> focuses() {
        return getEvents(FocusEvent.class);
    }

    @Override
    public void addFocusListener(FocusListener listener) {
        getEvents(FocusEvent.class).subscribe(listener::focus);
    }

    /**
     * @deprecated As of 7.0, replaced by
     *             {@link #addFocusListener(FocusListener)}
     */
    @Override
    @Deprecated
    public void addListener(FocusListener listener) {
        addFocusListener(listener);
    }

    @Override
    public void removeFocusListener(FocusListener listener) {
        throw new UnsupportedOperationException(
                "Focus listeners cannot currently be removed");
    }

    /**
     * @deprecated As of 7.0, replaced by
     *             {@link #removeFocusListener(FocusListener)}
     */
    @Override
    @Deprecated
    public void removeListener(FocusListener listener) {
        removeFocusListener(listener);
    }

    @Override
    public void focus() {
        super.focus();
    }

    @Override
    public int getTabIndex() {
        return getState(false).tabIndex;
    }

    @Override
    public void setTabIndex(int tabIndex) {
        getState().tabIndex = tabIndex;
    }

    /**
     * Returns the event bus used by this component.
     * 
     * @return the event bus
     */
    protected EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Returns the flow of events of the given type.
     * 
     * @param <E>
     *            the event type
     * @param klass
     *            the event class instance
     * @return the event flow
     */
    protected <E extends com.vaadin.server.react.events.Event> Flow<E> getEvents(
            Class<E> klass) {
        return getEventBus().events(klass);
    }

    @Override
    protected TabIndexState getState() {
        return (TabIndexState) super.getState();
    }

    @Override
    protected TabIndexState getState(boolean markAsDirty) {
        return (TabIndexState) super.getState(markAsDirty);
    }
}
