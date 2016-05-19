package com.vaadin.server.react;

import org.easymock.EasyMock;
import org.junit.Test;

import com.vaadin.server.react.Flow.Subscriber;

public class FlowTest {

    @Test
    public void testSubscriber() {
        verifyFlow(flow(), expect());
        verifyFlow(flow(1, 2, 3, 4), expect(1, 2, 3, 4));
    }

    @Test
    public void testSubscribers() {
        Flow<Integer> flow = flow(1, 2, 3, 4);
        verifyFlow(flow, expect(1, 2, 3, 4));
        verifyFlow(flow, expect(1, 2, 3, 4));
    }

    @Test
    public void testMap() {
        verifyFlow(flow(1, 2, 3, 4).map(i -> i * i), expect(1, 4, 9, 16));
    }

    @Test
    public void testFilter() {
        verifyFlow(flow(1, 2, 3, 4, 5, 6).filter(i -> i % 2 == 0),
                expect(2, 4, 6));
    }

    @Test
    public void testReduce() {
        verifyFlow(flow(1, 2, 3, 4).reduce((i, j) -> i + j, 0), expect(10));
    }

    @Test
    public void testFlatmap() {
        verifyFlow(flow(1, 2, 3, 4).flatMap(i -> Flow.from(i, 10 * i)),
                expect(1, 10, 2, 20, 3, 30, 4, 40));
    }

    @Test
    public void testCount() {
        verifyFlow(flow().count(), expect(0L));
        verifyFlow(flow(2, 4, 6).count(), expect(3L));
    }

    @Test
    public void testTakeWhile() throws Exception {
        verifyFlow(flow().takeWhile(x -> true), expect());
        verifyFlow(flow().takeWhile(x -> false), expect());

        verifyFlow(flow(1, 2, 3).takeWhile(x -> true), expect(1, 2, 3));
        verifyFlow(flow(1, 2, 3).takeWhile(x -> false), expect());

        verifyFlow(flow(1, 2, 3).takeWhile(x -> x % 2 != 0), expect(1));
    }

    @Test
    public void testDropWhile() throws Exception {
        verifyFlow(flow().dropWhile(x -> true), expect());
        verifyFlow(flow().dropWhile(x -> false), expect());

        verifyFlow(flow(1, 2, 3).dropWhile(x -> true), expect());
        verifyFlow(flow(1, 2, 3).dropWhile(x -> false), expect(1, 2, 3));

        verifyFlow(flow(1, 2, 3).dropWhile(x -> x % 2 != 0), expect(2, 3));
    }

    @Test
    public void testTake() {
        verifyFlow(flow().take(3), expect());
        verifyFlow(flow(1, 2, 3, 4).take(0), expect());
        verifyFlow(flow(1, 2, 3, 4).take(3), expect(1, 2, 3));
        verifyFlow(flow(1, 2, 3, 4).take(5), expect(1, 2, 3, 4));
    }

    @Test
    public void testDrop() {
        verifyFlow(flow().drop(3), expect());
        verifyFlow(flow(1, 2, 3, 4).drop(0), expect(1, 2, 3, 4));
        verifyFlow(flow(1, 2, 3, 4).drop(3), expect(4));
        verifyFlow(flow(1, 2, 3, 4).drop(5), expect());
    }

    protected <T> void verifyFlow(Flow<T> flow, Subscriber<? super T> sub) {
        EasyMock.replay(sub);
        flow.subscribe(sub);
        EasyMock.verify(sub);
    }

    @SuppressWarnings("unchecked")
    protected <T> Flow<T> flow(T... actual) {
        return Flow.from(actual);
    }

    @SuppressWarnings("unchecked")
    protected <T> Subscriber<T> expect(T... expected) {

        Subscriber<T> s = EasyMock.createStrictMock(Subscriber.class);
        for (T t : expected) {
            s.onNext(t);
        }
        s.onEnd();

        return s;
    }
}