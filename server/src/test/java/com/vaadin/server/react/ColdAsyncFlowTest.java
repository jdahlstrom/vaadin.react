package com.vaadin.server.react;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.easymock.EasyMock;

import com.vaadin.server.react.Flow.Subscriber;
import com.vaadin.server.react.impl.FlowImpl;

public class ColdAsyncFlowTest extends FlowTest {

    private ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> future;

    @Override
    protected <T> void verifyFlow(Flow<T> flow, Subscriber<? super T> sub) {
        EasyMock.replay(sub);
        flow.subscribe(sub);

        try {
            future.get(1, TimeUnit.SECONDS);
        } catch (CancellationException e) {
            // expected
        } catch (Error | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError("failed waiting for task:"
                    + e.getMessage(), e);
        }

        EasyMock.verify(sub);
    }

    @SuppressWarnings("unchecked")
    protected <T> Flow<T> flow(T... actual) {
        return new FlowImpl<T>(sub -> {
            future = exec.scheduleAtFixedRate(new Runnable() {
                int i = 0;

                @Override
                public void run() {
                    if (!sub.isSubscribed()) {
                        future.cancel(false);
                    } else if (i >= actual.length) {
                        sub.onEnd();
                        future.cancel(false);
                    } else {
                        sub.onNext(actual[i++]);
                    }
                }
            }, 10, 10, TimeUnit.MILLISECONDS);
        });
    }
}
