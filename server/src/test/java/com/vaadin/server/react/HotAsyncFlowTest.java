package com.vaadin.server.react;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.easymock.EasyMock;

import com.vaadin.server.react.impl.FlowImpl;

public class HotAsyncFlowTest extends FlowTest {

    private ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
    private Map<Runnable, Future<?>> executions = new HashMap<>();

    @Override
    protected <T> void verifyFlow(Flow<T> flow, Subscriber<? super T> sub) {

        EasyMock.replay(sub);
        flow.subscribe(sub);

        for (Entry<Runnable, Future<?>> entry : executions.entrySet()) {
            entry.setValue(exec.scheduleAtFixedRate(entry.getKey(), 10, 10,
                    TimeUnit.MILLISECONDS));
        }

        try {
            for (Future<?> f : executions.values()) {
                try {
                    f.get(TIMEOUT_SEC, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    throw e.getCause();
                } catch (CancellationException e) {
                    // expected
                }
            }
        } catch (Throwable e) {
            throw new AssertionError("Task failed", e);
        }

        EasyMock.verify(sub);
    }

    @Override
    protected <T> void verifyFlow(Flow<T> flow,
            Supplier<Subscriber<? super T>> supp) {
        verifyFlow(flow, supp.get());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> Flow<T> flow(T... actual) {

        Set<Subscriber<? super T>> subs = new LinkedHashSet<>();

        Runnable r = new Runnable() {
            int i = 0;

            @Override
            public void run() {
                synchronized (subs) {
                    if (i < actual.length) {
                        for (Subscriber<? super T> s : subs) {
                            if (s.isSubscribed()) {
                                s.onNext(actual[i]);
                            }
                        }
                        i++;
                    } else {
                        for (Subscriber<? super T> s : subs) {
                            if (s.isSubscribed()) {
                                s.onEnd();
                            }
                        }
                        subs.clear();
                        executions.get(this).cancel(false);
                    }
                }
            }
        };

        executions.put(r, null);

        return new FlowImpl<T>(s -> {
            synchronized (subs) {
                subs.add(s);
            }
        });
    }
}
