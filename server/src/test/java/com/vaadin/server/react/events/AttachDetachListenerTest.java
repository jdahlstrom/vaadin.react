package com.vaadin.server.react.events;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.function.Consumer;

import org.junit.Test;

import com.vaadin.server.ClientConnector.AttachEvent;
import com.vaadin.server.ClientConnector.AttachListener;
import com.vaadin.server.ClientConnector.DetachEvent;
import com.vaadin.server.ClientConnector.DetachListener;
import com.vaadin.tests.util.MockUI;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;

public class AttachDetachListenerTest {

    @Test
    public void testLegacyListeners() {

        Consumer<AttachEvent> onAttach = createConsumer();
        Consumer<DetachEvent> onDetach = createConsumer();

        onAttach.accept(anyObject(AttachEvent.class));
        onDetach.accept(anyObject(DetachEvent.class));

        replay(onAttach, onDetach);

        UI ui = new MockUI();
        Button b = new Button();

        AttachListener al = onAttach::accept;
        DetachListener dl = onDetach::accept;

        b.addAttachListener(al);
        b.addDetachListener(dl);

        ui.setContent(b);
        ui.setContent(null);

        b.removeAttachListener(al);
        b.removeDetachListener(dl);

        ui.setContent(b);
        ui.setContent(null);

        verify(onAttach, onDetach);
    }

    @SuppressWarnings("unchecked")
    private <T> Consumer<T> createConsumer() {
        return createStrictMock(Consumer.class);
    }
}
