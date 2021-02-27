package com.root;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CacheBuilder {

    private long maximumSize = 100_000;
    private long expireAfter;
    private CACHE_TYPE cacheType;
    private final List<RemovalListener> removalListeners = new ArrayList<>();

    public static CacheBuilder ofLru() {
        CacheBuilder cacheBuilder = new CacheBuilder();
        cacheBuilder.cacheType = CACHE_TYPE.LRU;
        return cacheBuilder;
    }

    public static CacheBuilder ofLfu() {
        CacheBuilder cacheBuilder = new CacheBuilder();
        cacheBuilder.cacheType = CACHE_TYPE.LFU;
        return cacheBuilder;
    }

    public CacheBuilder maximumSize(long maximumSize) {
        if (maximumSize <= 0) {
            throw new IllegalArgumentException("Maximum size value should be grater than 0");
        }
        this.maximumSize = maximumSize;
        return this;
    }

    public CacheBuilder expireAfterAccess(long expireAfter) {
        if (expireAfter <= 0) {
         throw new IllegalArgumentException("Expire after value should be grater than 0");
        }
        this.expireAfter = expireAfter;
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
