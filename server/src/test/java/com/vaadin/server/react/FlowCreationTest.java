package com.vaadin.server.react;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import com.vaadin.server.react.harnesses.FlowTestHarness;
import com.vaadin.server.react.harnesses.SyncFlowTestHarness;

public class FlowCreationTest extends FlowTestBase {

    @Rule
    public Timeout timeout = new Timeout(1000 * FlowTestHarness.TIMEOUT_SEC);

    @Test
    public void testFromStream() {
        Flow<Integer> flow = Flow
                .from(Arrays.stream(new int[] { 1, 2, 3 }));

        verifyFlow(flow, expect(1, 2, 3).get());
        // Stream can only be consumed once
        verifyFlow(flow, expect().get());

        flow = Flow
                .from(Arrays.stream(new int[] { 1, 2, 3 }));

        verifyFlow(flow, expectAndUnsubscribe(1).get());
        verifyFlow(flow, expectAndUnsubscribe().get());
    }

    @Test
    public void testFromFuture() {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        Flow<Integer> flow = Flow.from(future);
        future.complete(42);
        verifyFlow(flow, expect(42));
        verifyFlow(flow, expectAndUnsubscribe(42));

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

        // Infinite flow
        flow = Flow.generate(() -> Optional.of(42));

        verifyFlow(flow.take(4), expect(42, 42, 42, 42));
        verifyFlow(flow, expectAndUnsubscribe(42, 42, 42).get());
    }

    @Test
    public void testIterate() {
        Flow<Integer> flow = Flow.iterate(1, i -> {
            return i < 5 ? Optional.of(2 * i) : Optional.empty();
        });

        verifyFlow(flow, expect(1, 2, 4, 8));
        verifyFlow(flow, expectAndUnsubscribe(1, 2));

        // Infinite flow
        flow = Flow.iterate(1, i -> Optional.of(i + 2));

        verifyFlow(flow.takeWhile(i -> i < 10), expect(1, 3, 5, 7, 9));
        verifyFlow(flow, expectAndUnsubscribe(1, 3).get());
    }

    @Override
    protected FlowTestHarness getHarness() {
        return harness;
    }

    private FlowTestHarness harness = new SyncFlowTestHarness();
}
