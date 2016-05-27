package com.vaadin.server.react;

import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.vaadin.server.react.harnesses.ColdAsyncFlowTestHarness;
import com.vaadin.server.react.harnesses.FlowTestHarness;
import com.vaadin.server.react.harnesses.HotAsyncFlowTestHarness;
import com.vaadin.server.react.harnesses.SyncFlowTestHarness;

@RunWith(Parameterized.class)
public class FlowCombinatorsTest extends FlowTestBase {

    @Parameter
    public FlowTestHarness harness;

    @Parameters(name = "{0}")
    public static List<FlowTestHarness[]> harnesses() {
        return Arrays
                .asList(new FlowTestHarness[][] {
                        { new SyncFlowTestHarness() },
                        { new ColdAsyncFlowTestHarness() },
                        { new HotAsyncFlowTestHarness() } });
    }

    @Rule
    public Timeout timeout = new Timeout(1000 * FlowTestHarness.TIMEOUT_SEC);

    @Test
    public void testSubscriber() {
        verifyFlow(flow(), expect());
        verifyFlow(flow(1, 2, 3, 4), expect(1, 2, 3, 4));
        verifyFlow(flow(1, 2, 3), expectAndUnsubscribe());
    }

    @Test
    public void testMerge() {
        verifyFlow(Flow.merge(flow()), expect());

        verifyFlow(Flow.merge(flow(), flow()), expect());

        verifyFlow(Flow.merge(flow(1, 2, 3), flow()), expect(1, 2, 3));
        verifyFlow(Flow.merge(flow(), flow(1, 2, 3)), expect(1, 2, 3));

        verifyFlow(Flow.merge(flow(1, 2, 3), flow(6, 5, 4)),
                expectMerged(values(1, 2, 3),
                        values(6, 5, 4)));

        verifyFlow(Flow.merge(flow(1), flow(), flow(2)),
                expectMerged(values(1), values(2)));

        verifyFlow(Flow.merge(flow(1, 2), flow(3), flow(4, 5, 6)),
                expectMerged(values(1, 2), values(3), values(4, 5, 6)));
    }

    @Test
    public void testMap() {
        verifyFlow(flow().map(o -> "" + o), expect());

        verifyFlow(flow(1, 2, 3, 4).map(i -> i * i), expect(1, 4, 9, 16));
        verifyFlow(flow(1, 2, 3, 4).map(i -> "" + i),
                expect("1", "2", "3", "4"));

        verifyFlow(flow(1, 2, 3).map(i -> i + 2), expectAndUnsubscribe(3));
    }

    @Test
    public void testFilter() {
        verifyFlow(flow().filter(o -> true), expect());

        verifyFlow(flow(1, 2, 3, 4, 5, 6).filter(i -> i % 2 == 0),
                expect(2, 4, 6));

        verifyFlow(flow(1, 2, 3).filter(i -> i < 3), expectAndUnsubscribe(1));
    }

    @Test
    public void testReduce() {
        verifyFlow(flow().reduce((a, b) -> "" + a + b, ""), expect(""));

        verifyFlow(flow(1, 2, 3, 4).reduce((i, j) -> i + j, 0), expect(10));

        verifyFlow(flow(1, 2, 3).reduce((i, j) -> i + j, 0),
                expectAndUnsubscribe(6));
    }

    @Test
    public void testFlatmap() {
        // TODO: Test with async subflows too

        verifyFlow(flow().flatMap(i -> Flow.of('a', 'b')), expect());

        verifyFlow(flow(1, 2, 3, 4).flatMap(i -> Flow.of(i, 10 * i)),
                expect(1, 10, 2, 20, 3, 30, 4, 40));

        verifyFlow(flow(1, 2, 3).flatMap(i -> Flow.of(i + 2)),
                expectAndUnsubscribe(3));
    }

    @Test
    public void testCount() {
        verifyFlow(flow().count(), expect(0L));
        verifyFlow(flow(2, 4, 6).count(), expect(3L));
        verifyFlow(flow(1, 2).count(), expectAndUnsubscribe(2L));
    }

    @Test
    public void testAny() {
        verifyFlow(flow().anyMatch(x -> false), expect(false));
        verifyFlow(flow().anyMatch(x -> true), expect(false));

        verifyFlow(flow(1).anyMatch(x -> false), expect(false));
        verifyFlow(flow(1).anyMatch(x -> true), expect(true));

        verifyFlow(flow(1, 2, 3).anyMatch(x -> x % 2 == 0), expect(true));
        verifyFlow(flow(1, 2, 3).anyMatch(x -> x < 0), expect(false));

        verifyFlow(flow(1, 2, 3).anyMatch(x -> x < 2),
                expectAndUnsubscribe(true));
    }

    @Test
    public void testAll() {
        verifyFlow(flow().allMatch(x -> false), expect(true));
        verifyFlow(flow().allMatch(x -> true), expect(true));

        verifyFlow(flow(1).allMatch(x -> false), expect(false));
        verifyFlow(flow(1).allMatch(x -> true), expect(true));

        verifyFlow(flow(1, 2, 3).allMatch(x -> x % 2 == 0), expect(false));
        verifyFlow(flow(1, 2, 3).allMatch(x -> x < 4), expect(true));

        verifyFlow(flow(1, 2, 3).allMatch(x -> x < 0),
                expectAndUnsubscribe(false));
    }

    @Test
    public void testNone() {
        verifyFlow(flow().noneMatch(x -> false), expect(true));
        verifyFlow(flow().noneMatch(x -> true), expect(true));

        verifyFlow(flow(1).noneMatch(x -> false), expect(true));
        verifyFlow(flow(1).noneMatch(x -> true), expect(false));

        verifyFlow(flow(1, 2, 3).noneMatch(x -> x % 2 == 0), expect(false));
        verifyFlow(flow(1, 2, 3).noneMatch(x -> x < 0), expect(true));

        verifyFlow(flow(1, 2, 3).noneMatch(x -> x < 2),
                expectAndUnsubscribe(false));
    }

    @Test
    public void testTakeWhile() throws Exception {
        verifyFlow(flow().takeWhile(x -> true), expect());
        verifyFlow(flow().takeWhile(x -> false), expect());
        //
        verifyFlow(flow(1, 2, 3).takeWhile(x -> true), expect(1, 2, 3));
        verifyFlow(flow(1, 2, 3).takeWhile(x -> false), expect());
        //
        verifyFlow(flow(1, 2, 3).takeWhile(x -> x % 2 != 0), expect(1));
        //
        verifyFlow(flow(1, 2, 3).takeWhile(x -> x < 3),
                expectAndUnsubscribe(1));
    }

    @Test
    public void testSkipWhile() {
        verifyFlow(flow().skipWhile(x -> true), expect());
        verifyFlow(flow().skipWhile(x -> false), expect());

        verifyFlow(flow(1, 2, 3).skipWhile(x -> true), expect());
        verifyFlow(flow(1, 2, 3).skipWhile(x -> false), expect(1, 2, 3));

        verifyFlow(flow(1, 2, 3).skipWhile(x -> x % 2 != 0), expect(2, 3));

        verifyFlow(flow(1, 2, 3).skipWhile(x -> x < 2),
                expectAndUnsubscribe(2));
    }

    @Test
    public void testTake() {
        verifyFlow(flow().take(3), expect());

        verifyFlow(flow(1, 2, 3, 4).take(0), expect());
        verifyFlow(flow(1, 2, 3, 4).take(3), expect(1, 2, 3));
        verifyFlow(flow(1, 2, 3, 4).take(5), expect(1, 2, 3, 4));

        verifyFlow(flow(1, 2, 3).take(2), expectAndUnsubscribe(1));
    }

    @Test
    public void testSkip() {
        verifyFlow(flow().skip(3), expect());

        verifyFlow(flow(1, 2, 3, 4).skip(0), expect(1, 2, 3, 4));
        verifyFlow(flow(1, 2, 3, 4).skip(3), expect(4));
        verifyFlow(flow(1, 2, 3, 4).skip(5), expect());

        verifyFlow(flow(1, 2, 3).skip(1), expectAndUnsubscribe(2));
    }

    @Override
    protected FlowTestHarness getHarness() {
        return harness;
    }
}
