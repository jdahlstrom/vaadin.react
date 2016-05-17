package com.vaadin.server.react.events;

import java.util.function.Consumer;

import org.easymock.EasyMock;
import org.junit.Test;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;

public class EventBusTest {

    class TestEvent implements Event {
        String id;

        TestEvent(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + id + "]";
        }
    }

    @Test
    public void testEventBus() {
        EventBus bus = new EventBus();

        TestEvent first = new TestEvent("first");
        TestEvent second = new TestEvent("second");
        TestEvent third = new TestEvent("third");

        Consumer<TestEvent> sub1 = createConsumer();
        Consumer<TestEvent> sub2 = createConsumer();

        sub1.accept(second);
        sub1.accept(third);
        sub2.accept(third);

        EasyMock.replay(sub1, sub2);

        bus.fireEvent(first);

        bus.events(TestEvent.class).subscribe(sub1);

        bus.fireEvent(second);

        bus.events(TestEvent.class).subscribe(sub2);

        bus.fireEvent(third);

        EasyMock.verify(sub1, sub2);
    }

    private <T> Consumer<T> createConsumer() {
        return EasyMock.createStrictMock(Consumer.class);
    }

    @Test
    public void testButton() throws Exception {
        Button b = new Button();

        Consumer<ClickEvent> sub1 = createConsumer();

        sub1.accept(EasyMock.anyObject(ClickEvent.class));
        sub1.accept(EasyMock.anyObject(ClickEvent.class));

        EasyMock.replay(sub1);

        b.clicks().subscribe(sub1);

        b.click();
        b.click();

        EasyMock.verify(sub1);
    }
}
