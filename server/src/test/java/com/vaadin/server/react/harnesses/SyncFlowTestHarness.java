package com.vaadin.server.react.harnesses;

import java.util.function.Supplier;

import com.vaadin.server.react.Flow;
import com.vaadin.server.react.Subscriber;

public class SyncFlowTestHarness extends FlowTestHarness {

    @Override
    @SuppressWarnings("unchecked")
    public <T> Flow<T> flow(T... actual) {
        return Flow.of(actual);
    }

    @Override
    public <T> void verifyFlow(Flow<T> flow,
            Supplier<Subscriber<? super T>> subSup) {
        for (int i = 0; i < 2; i++) {
            verifyFlow(flow, subSup.get());
        }
    }
}
