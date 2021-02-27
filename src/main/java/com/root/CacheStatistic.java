package com.root;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.concurrent.atomic.AtomicLong;

@Setter
@Getter
@ToString
public class CacheStatistic {
    private long averageTimePutting;
    private long addCount;
    private long evictionsCount;

    public void addTimePutting(long stopAndGet) {
        this.averageTimePutting = (averageTimePutting * addCount + stopAndGet) / (addCount + 1);
        this.addCount = addCount + 1;
    }

    public void addEviction() {
        this.evictionsCount++;
    }
}
