package com.root.util;

import java.time.Duration;

import static java.time.temporal.ChronoUnit.NANOS;

public class StopWatcher {
    private final long start;

    public StopWatcher(long start) {
        this.start = start;
    }

    public static StopWatcher createStarted() {
        return new StopWatcher(System.nanoTime());
    }

    public Duration stopAndGet(){
        return Duration.of(System.nanoTime() - start, NANOS);
    }
}
