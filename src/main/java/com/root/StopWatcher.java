package com.root;

public class StopWatcher {
    private final long start;

    public StopWatcher(long start) {
        this.start = start;
    }

    public static StopWatcher createStarted() {
        return new StopWatcher(System.currentTimeMillis());
    }

    public long stopAndGet(){
        return System.currentTimeMillis() - start;
    }
}
