package com.vaadin.server.react;

import java.util.function.Supplier;

import com.vaadin.server.react.harnesses.FlowTestHarness;

public abstract class FlowTestBase {

    protected abstract FlowTestHarness getHarness();

    @SuppressWarnings("unchecked")
    protected <T> T[] values(T... values) {
        return values;
    }

    @SuppressWarnings("unchecked")
    protected <T> Flow<T> flow(T... actual) {
        return getHarness().flow(actual);
    }

    protected <T> void verifyFlow(Flow<T> flow,
            Supplier<Subscriber<? super T>> supp) {
        getHarness().verifyFlow(flow, supp);

    }

    protected <T> void verifyFlow(Flow<T> flow, Subscriber<? super T> sub) {
        getHarness().verifyFlow(flow, sub);
    }

    @SuppressWarnings("unchecked")
    protected <T> Supplier<Subscriber<? super T>> expect(T... values) {
        return getHarness().expect(values);
    }

    @SuppressWarnings("unchecked")
    protected <T> Supplier<Subscriber<? super T>> expectAndUnsubscribe(
            T... values) {
        return getHarness().expectAndUnsubscribe(values);
    }

    protected <T> Supplier<Subscriber<? super T>> expectError(Exception e) {
        return getHarness().expectError(e);
    }

    @SuppressWarnings("unchecked")
    protected <T> Subscriber<? super T> expectMerged(
            T[]... values) {
        return getHarness().expectMerged(values);
    }
}
