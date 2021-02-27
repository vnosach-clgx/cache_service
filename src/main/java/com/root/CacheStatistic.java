package com.root;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Duration;

import static java.time.Duration.ZERO;

@Setter
@Getter
@ToString
public class CacheStatistic {
    private Duration averageTimePutting = ZERO;
    private long addCount;
    private long evictionsCount;

    public void addTimePutting(Duration stopAndGet) {
        this.averageTimePutting = averageTimePutting.multipliedBy(addCount).plus(stopAndGet).dividedBy(addCount + 1);
        this.addCount = addCount + 1;
    }

    public void addEviction() {
        this.evictionsCount++;
    }
}
