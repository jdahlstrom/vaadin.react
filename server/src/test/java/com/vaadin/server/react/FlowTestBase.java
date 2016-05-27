package com.vaadin.server.react;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Arrays;
import java.util.OptionalInt;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;

import com.vaadin.server.react.Flow.Subscription;

public abstract class FlowTestBase {

    protected <T> T[] values(@SuppressWarnings("unchecked") T... values) {
        return values;
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
            Subscriber<T> s = subscriber();
            for (T t : expected) {
                s.onNext(t);
            }
            s.onEnd();
            return s;
        };
    }

    protected <T> Subscriber<? super T> expectMerged(
            @SuppressWarnings("unchecked") T[]... values) {

        class MergeMatcher {
            int[] indices = new int[values.length];

            T value(int i) {
                return values[i][indices[i]];
            }

            IntStream indices() {
                return IntStream
                        .range(0, values.length)
                        .filter(i -> indices[i] < values[i].length);
            }

            T match() {
                EasyMock.reportMatcher(new IArgumentMatcher() {
                    @Override
                    public boolean matches(Object value) {
                        OptionalInt match = indices()
                                .filter(i -> value(i).equals(value))
                                .findAny();
                        match.ifPresent(i -> indices[i]++);
                        return match.isPresent();
                    }

                    @Override
                    public void appendTo(StringBuffer buffer) {
                        String joined = indices()
                                .mapToObj(i -> "" + value(i))
                                .collect(Collectors.joining(" | "));
                        buffer.append(joined);
                    }
                });
                return null;
            }
        }

        MergeMatcher matcher = new MergeMatcher();

        Subscriber<T> s = subscriber();

        int len = Arrays.stream(values).mapToInt(x -> x.length).sum();
        for (int i = 0; i < len; i++) {
            s.onNext(matcher.match());
        }
        s.onEnd();
        return s;
    }

    @SuppressWarnings("unchecked")
    protected <T> Supplier<Subscriber<? super T>> expectAndUnsubscribe(
            T... expected) {
        return () -> {
            Subscriber<T> s = subscriber();
            for (T t : expected) {
                s.onNext(t);
            }
            // Unsubscribe, verify no subsequent calls are made
            EasyMock.expect(s.isSubscribed()).andReturn(false).atLeastOnce();
            return s;
        };
    }

    /**
     * @return a mock Subscriber
     */
    protected <T> Subscriber<T> subscriber() {
        @SuppressWarnings("unchecked")
        Subscriber<T> s = createStrictMock(Subscriber.class);
        EasyMock.expect(s.isSubscribed()).andReturn(false).anyTimes();
        s.onSubscribe(anyObject(Subscription.class));
        EasyMock.expect(s.isSubscribed()).andStubReturn(true);
        return s;
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
