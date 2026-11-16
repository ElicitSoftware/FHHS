package com.elicitsoftware.test;

/*-
 * ***LICENSE_START***
 * Elicit FHHS
 * %%
 * Copyright (C) 2025 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Keeps every span the application ends during a {@code @QuarkusTest}, so that a test can read
 * what a trace of a request would hold (UC-006 BR-004). Quarkus adds any {@link SpanProcessor}
 * bean to its tracer; this one exists on the test classpath only.
 */
@Singleton
@Unremovable
public class RecordedSpans implements SpanProcessor {

    private final List<SpanData> spans = new CopyOnWriteArrayList<>();

    /** The spans ended since the last {@link #clear()}. */
    public List<SpanData> spans() {
        return List.copyOf(spans);
    }

    /** Forgets what was recorded. */
    public void clear() {
        spans.clear();
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        // only ended spans are kept
    }

    @Override
    public boolean isStartRequired() {
        return false;
    }

    @Override
    public void onEnd(ReadableSpan span) {
        spans.add(span.toSpanData());
    }

    @Override
    public boolean isEndRequired() {
        return true;
    }
}
