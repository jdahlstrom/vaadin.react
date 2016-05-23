package com.vaadin.server.react;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

import org.easymock.EasyMock;

import com.vaadin.server.react.impl.FlowImpl;

public class ColdAsyncFlowTest extends FlowTest {

    private Timer timer = new Timer();
    private volatile boolean ended = true;

    @Override
    protected <T> void verifyFlow(Flow<T> flow,
            Supplier<Subscriber<? super T>> subSup) {

        Subscriber<? super T> sub = subSup.get();

        EasyMock.replay(sub);
        flow.subscribe(sub);

        while (!ended) {
        }

        EasyMock.verify(sub);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> Flow<T> flow(T... actual) {
        return new FlowImpl<T>(s -> {
            ended = false;
            timer.schedule(new TimerTask() {
                int i = 0;

                @Override
                public void run() {
                    try {
                        if (i < actual.length) {
                            s.onNext(actual[i++]);
                        } else {
                            s.onEnd();
                            cancel();
                            ended = true;
                        }
                    } catch (Throwable e) {
                        cancel();
                        ended = true;
                        throw e;
                    }
                }
            }, 10, 10);
        });
    }
}
