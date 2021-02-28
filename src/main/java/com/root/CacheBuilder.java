package com.root;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.root.CacheBuilder.CACHE_TYPE.LFU;
import static com.root.CacheBuilder.CACHE_TYPE.LRU;

@Getter
public class CacheBuilder {

    private long maximumSize;
    private long expireAfter;
    private CACHE_TYPE cacheType;
    private final List<RemovalListener> removalListeners = new ArrayList<>();

    public static CacheBuilder lru() {
        CacheBuilder cacheBuilder = new CacheBuilder();
        cacheBuilder.cacheType = LRU;
        return cacheBuilder;
    }

    public static CacheBuilder lfu() {
        CacheBuilder cacheBuilder = new CacheBuilder();
        cacheBuilder.cacheType = LFU;
        return cacheBuilder;
    }

    public CacheBuilder maximumSize(long maximumSize) {
        if (maximumSize <= 0) {
            throw new IllegalArgumentException("Maximum size value should be grater than 0");
        }
        this.maximumSize = maximumSize;
        return this;
    }

    public CacheBuilder expireAfterAccess(long expireAfter, TimeUnit timeUnit) {
        if (expireAfter <= 0) {
         throw new IllegalArgumentException("Expire after value should be grater than 0");
        }
        this.expireAfter = timeUnit.toMillis(expireAfter);
        return this;
    }

    public CacheBuilder removalListener(RemovalListener removalListener) {
        this.removalListeners.add(removalListener);
        return this;
    }

    public Cache build() {
        return new Cache(this);
    }

    enum CACHE_TYPE {
        LRU,
        LFU
    }

}
