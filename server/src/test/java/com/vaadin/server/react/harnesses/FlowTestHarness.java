package com.vaadin.server.react.harnesses;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createStrictMock;

import java.util.Arrays;
import java.util.OptionalInt;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;

import com.vaadin.server.react.Flow;
import com.vaadin.server.react.Flow.Subscription;
import com.vaadin.server.react.Subscriber;

public abstract class FlowTestHarness {

    public static final int TIMEOUT_SEC = 5;

    @SuppressWarnings("unchecked")
    public abstract <T> Flow<T> flow(T... actual);

    public abstract <T> void verifyFlow(Flow<T> flow,
            Supplier<Subscriber<? super T>> supp);

    public <T> void verifyFlow(Flow<T> flow, Subscriber<? super T> sub) {
        EasyMock.replay(sub);
        flow.subscribe(sub);
        EasyMock.verify(sub);
    }

    public <T> Subscriber<T> subscriber() {
        @SuppressWarnings("unchecked")
        Subscriber<T> s = createStrictMock(Subscriber.class);
        EasyMock.expect(s.isSubscribed()).andReturn(false).anyTimes();
        s.onSubscribe(anyObject(Subscription.class));
        EasyMock.expect(s.isSubscribed()).andStubReturn(true);
        return s;
    }

    @SuppressWarnings("unchecked")
    public <T> Supplier<Subscriber<? super T>> expect(T... expected) {
        return () -> {
            Subscriber<T> s = subscriber();
            for (T t : expected) {
                s.onNext(t);
            }
            s.onEnd();
            return s;
        };
    }

    @SuppressWarnings("unchecked")
    public <T> Supplier<Subscriber<? super T>> expectAndUnsubscribe(
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

    public <T> Supplier<Subscriber<? super T>> expectError(Exception e) {
        return () -> {
            @SuppressWarnings("unchecked")
            Subscriber<T> s = createStrictMock(Subscriber.class);
            EasyMock.expect(s.isSubscribed()).andReturn(false).anyTimes();
            s.onSubscribe(anyObject(Subscription.class));
            EasyMock.expect(s.isSubscribed()).andStubReturn(true);
            s.onError(e);
            s.onEnd();
            return s;
        };
    }

    @SuppressWarnings("unchecked")
    public <T> Subscriber<? super T> expectMerged(T[]... values) {

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

    @Override
    public String toString() {
        return getClass().getSimpleName().replace("FlowTestHarness", "");
    }
}
