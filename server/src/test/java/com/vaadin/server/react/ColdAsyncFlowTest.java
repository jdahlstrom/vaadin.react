package com.vaadin.server.react;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.easymock.EasyMock;

import com.vaadin.server.react.impl.FlowImpl;

public class ColdAsyncFlowTest extends FlowTest {

    private ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);

    @Override
    protected <T> void verifyFlow(Flow<T> flow,
            Supplier<Subscriber<? super T>> subSup) {

        Subscriber<? super T> sub = subSup.get();

        EasyMock.replay(sub);
        flow.subscribe(sub);
        EasyMock.verify(sub);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> Flow<T> flow(T... actual) {

        return new FlowImpl<T>(new Consumer<Subscriber<? super T>>() {

            Future<?> f;
            int i = 0;

            @Override
            public void accept(Subscriber<? super T> sub) {

                f = exec.scheduleAtFixedRate(() -> {
                    if (!sub.isSubscribed()) {
                        f.cancel(false);
                    } else if (i >= actual.length) {
                        sub.onEnd();
                        f.cancel(false);
                    } else {
                        sub.onNext(actual[i++]);
                    }
                }, 10, 10, TimeUnit.MILLISECONDS);

                try {
                    f.get();
                } catch (CancellationException e) {
                    // expected
                } catch (ExecutionException e) {
                    throw new AssertionError("Task failed", e.getCause());
                } catch (Exception e) {
                    throw new AssertionError("Unexpected error", e);
                }
            }
        });
    }
}
