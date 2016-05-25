package com.vaadin.server.react.events;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.function.Consumer;

import org.junit.Test;

import com.vaadin.server.react.Flow.Subscription;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

public class ButtonClickListenerTest {

    @Test
    public void testClickListeners() {
        Button b = new Button();

        Consumer<ClickEvent> onClick = createConsumer();

        onClick.accept(anyObject(ClickEvent.class));
        expectLastCall().times(2);

        replay(onClick);

        Subscription s = b.clicks().subscribe(onClick);

        b.click();
        b.click();

        s.unsubscribe();

        b.click();

        verify(onClick);
    }

    @Test
    public void testLegacyClickListeners() {

        Consumer<ClickEvent> onClick = createConsumer();

        onClick.accept(anyObject(ClickEvent.class));
        expectLastCall().times(2);

        replay(onClick);

        Button b = new Button();

        ClickListener l = e -> onClick.accept(e);
        b.addClickListener(l);

        b.click();
        b.click();

        b.removeClickListener(l);

        b.click();

        verify(onClick);
    }

    @SuppressWarnings("unchecked")
    private <T> Consumer<T> createConsumer() {
        return createStrictMock(Consumer.class);
    }
}
