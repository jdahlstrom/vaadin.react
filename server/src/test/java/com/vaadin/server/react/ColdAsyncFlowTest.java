package com.vaadin.server.react;

import java.util.Timer;
import java.util.TimerTask;

import org.easymock.EasyMock;

import com.vaadin.server.react.Flow.Subscriber;
import com.vaadin.server.react.impl.FlowImpl;

public class ColdAsyncFlowTest extends FlowTest {

    private Timer timer = new Timer();
    private volatile boolean ended = false;

    @Override
    protected <T> void verifyFlow(Flow<T> flow, Subscriber<? super T> sub) {
        EasyMock.replay(sub);
        flow.subscribe(sub);

        while (!ended) {
        }
        ended = false;

        EasyMock.verify(sub);
    }

    @SuppressWarnings("unchecked")
    protected <T> Flow<T> flow(T... actual) {
        return new FlowImpl<T>(s -> {
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
