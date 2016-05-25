package com.vaadin.server.react.events;

import java.util.function.Consumer;

import org.easymock.EasyMock;
import org.junit.Test;

import com.vaadin.server.react.Flow.Subscription;

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
        TestEvent fourth = new TestEvent("fourth");

        Consumer<TestEvent> sub1 = createConsumer();
        Consumer<TestEvent> sub2 = createConsumer();

        sub1.accept(second);
        sub1.accept(third);
        sub2.accept(third);
        sub2.accept(fourth);

        EasyMock.replay(sub1, sub2);

        bus.fireEvent(first);

        Subscription s = bus.events(TestEvent.class).subscribe(sub1);

        bus.fireEvent(second);

        bus.events(TestEvent.class).subscribe(sub2);

        bus.fireEvent(third);

        s.unsubscribe();

        bus.fireEvent(fourth);

        EasyMock.verify(sub1, sub2);
    }

    @SuppressWarnings("unchecked")
    private <T> Consumer<T> createConsumer() {
        return EasyMock.createStrictMock(Consumer.class);
    }
}
