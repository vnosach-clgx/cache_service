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

    public void puttingDuration(Duration lastPutDuration) {
        this.averageTimePutting = averageTimePutting.multipliedBy(addCount).plus(lastPutDuration).dividedBy(addCount + 1);
        this.addCount++;
    }

    public void increaseEvictionCount() {
        this.evictionsCount++;
    }
}
