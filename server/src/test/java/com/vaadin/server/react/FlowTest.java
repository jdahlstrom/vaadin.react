package com.vaadin.server.react;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.easymock.EasyMock;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import com.vaadin.server.react.Flow.Subscription;

public class FlowTest {

    @Rule
    public Timeout timeout = new Timeout(1000);

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
    public void testFromStream() {
        Flow<Integer> flow = Flow
                .from(Arrays.stream(new int[] { 1, 2, 3 }));

        verifyFlow(flow, expect(1, 2, 3).get());
        // Stream can only be consumed once
        verifyFlow(flow, expect().get());
    }

    @Test
    public void testFromFuture() {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        Flow<Integer> flow = Flow.from(future);
        future.complete(42);
        verifyFlow(flow, expect(42));

        future = new CompletableFuture<>();
        flow = Flow.from(future);
        Exception e = new Exception();
        future.completeExceptionally(e);
        verifyFlow(flow, expectError(e));
    }

    @Test
    public void testGenerate() {
        int[] counter = new int[] { 0 };
        Flow<Integer> flow = Flow.generate(() -> {
            return counter[0] < 5 ? Optional.of(counter[0]++) : Optional
                    .empty();
        });

        verifyFlow(flow, expect(0, 1, 2, 3, 4).get());
        verifyFlow(flow, expect().get());
    }

    @Test
    public void testIterate() {
        Flow<Integer> flow = Flow.iterate(1, i -> {
            return i < 5 ? Optional.of(2 * i) : Optional.empty();
        });

        verifyFlow(flow, expect(1, 2, 4, 8));
    }

    @Test
    public void testMap() {
        verifyFlow(flow().map(o -> "" + o), expect());
        verifyFlow(flow(1, 2, 3, 4).map(i -> i * i), expect(1, 4, 9, 16));
        verifyFlow(flow(1, 2, 3, 4).map(i -> "" + i),
                expect("1", "2", "3", "4"));
    }

    @Test
    public void testFilter() {
        verifyFlow(flow().filter(o -> true), expect());
        verifyFlow(flow(1, 2, 3, 4, 5, 6).filter(i -> i % 2 == 0),
                expect(2, 4, 6));
    }

    @Test
    public void testReduce() {
        verifyFlow(flow().reduce((a, b) -> "" + a + b, ""), expect(""));
        verifyFlow(flow(1, 2, 3, 4).reduce((i, j) -> i + j, 0), expect(10));
    }

    @Test
    public void testFlatmap() {
        verifyFlow(flow().flatMap(i -> Flow.of('a', 'b')), expect());
        verifyFlow(flow(1, 2, 3, 4).flatMap(i -> Flow.of(i, 10 * i)),
                expect(1, 10, 2, 20, 3, 30, 4, 40));
    }

    @Test
    public void testCount() {
        verifyFlow(flow().count(), expect(0L));
        verifyFlow(flow(2, 4, 6).count(), expect(3L));
    }

    @Test
    public void testAny() {
        verifyFlow(flow().anyMatch(x -> false), expect(false));
        verifyFlow(flow().anyMatch(x -> true), expect(false));

        verifyFlow(flow(1).anyMatch(x -> false), expect(false));
        verifyFlow(flow(1).anyMatch(x -> true), expect(true));

        verifyFlow(flow(1, 2, 3).anyMatch(x -> x % 2 == 0), expect(true));
        verifyFlow(flow(1, 2, 3).anyMatch(x -> x < 0), expect(false));
    }

    @Test
    public void testAll() {
        verifyFlow(flow().allMatch(x -> false), expect(true));
        verifyFlow(flow().allMatch(x -> true), expect(true));

        verifyFlow(flow(1).allMatch(x -> false), expect(false));
        verifyFlow(flow(1).allMatch(x -> true), expect(true));

        verifyFlow(flow(1, 2, 3).allMatch(x -> x % 2 == 0), expect(false));
        verifyFlow(flow(1, 2, 3).allMatch(x -> x < 4), expect(true));
    }

    @Test
    public void testNone() {
        verifyFlow(flow().noneMatch(x -> false), expect(true));
        verifyFlow(flow().noneMatch(x -> true), expect(true));

        verifyFlow(flow(1).noneMatch(x -> false), expect(true));
        verifyFlow(flow(1).noneMatch(x -> true), expect(false));

        verifyFlow(flow(1, 2, 3).noneMatch(x -> x % 2 == 0), expect(false));
        verifyFlow(flow(1, 2, 3).noneMatch(x -> x < 0), expect(true));
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
    public void testSkipWhile() {
        verifyFlow(flow().skipWhile(x -> true), expect());
        verifyFlow(flow().skipWhile(x -> false), expect());

        verifyFlow(flow(1, 2, 3).skipWhile(x -> true), expect());
        verifyFlow(flow(1, 2, 3).skipWhile(x -> false), expect(1, 2, 3));

        verifyFlow(flow(1, 2, 3).skipWhile(x -> x % 2 != 0), expect(2, 3));
    }

    @Test
    public void testTake() {
        verifyFlow(flow().take(3), expect());
        verifyFlow(flow(1, 2, 3, 4).take(0), expect());
        verifyFlow(flow(1, 2, 3, 4).take(3), expect(1, 2, 3));
        verifyFlow(flow(1, 2, 3, 4).take(5), expect(1, 2, 3, 4));
    }

    @Test
    public void testSkip() {
        verifyFlow(flow().skip(3), expect());
        verifyFlow(flow(1, 2, 3, 4).skip(0), expect(1, 2, 3, 4));
        verifyFlow(flow(1, 2, 3, 4).skip(3), expect(4));
        verifyFlow(flow(1, 2, 3, 4).skip(5), expect());
    }

    protected <T> void verifyFlow(Flow<T> flow,
            Subscriber<? super T> sub) {
        replay(sub);
        flow.subscribe(sub);
        verify(sub);
    }

    protected <T> void verifyFlow(Flow<T> flow,
            Supplier<Subscriber<? super T>> subSup) {

        for (int i = 0; i < 2; i++) {
            verifyFlow(flow, subSup.get());
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> Flow<T> flow(T... actual) {
        return Flow.of(actual);
    }

    @SuppressWarnings("unchecked")
    protected <T> Supplier<Subscriber<? super T>> expect(T... expected) {
        return () -> {
            Subscriber<T> s = createStrictMock(Subscriber.class);
            EasyMock.expect(s.isSubscribed()).andReturn(false).anyTimes();
            s.onSubscribe(anyObject(Subscription.class));

            EasyMock.expect(s.isSubscribed()).andStubReturn(true);
            for (T t : expected) {
                s.onNext(t);
            }
            s.onEnd();
            return s;
        };
    }

    @SuppressWarnings("unchecked")

    protected <T> Supplier<Subscriber<? super T>> expectError(Exception e) {
        return () -> {
            Subscriber<T> s = createStrictMock(Subscriber.class);
            EasyMock.expect(s.isSubscribed()).andReturn(false).anyTimes();
            s.onSubscribe(anyObject(Subscription.class));
            EasyMock.expect(s.isSubscribed()).andStubReturn(true);
            s.onError(e);
            s.onEnd();
            return s;
        };
    }
}
