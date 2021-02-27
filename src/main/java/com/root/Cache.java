package com.root;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.root.CacheBuilder.CACHE_TYPE.LRU;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
public class Cache {

    private final Map<Object, ValueWrapper> cacheMap;
    @Getter
    private final CacheStatistic cacheStatistic = new CacheStatistic();
    private final Lock lock = new ReentrantLock();

    private final List<CacheEvent> cacheEvents = new LinkedList();
    private final long expiryInMillis;
    private final long maximumSize;

    protected Cache(CacheBuilder cacheBuilder) {
        this.cacheMap = cacheBuilder.getCache_type().equals(LRU) ? new LruHashMap() : new LfuHashMap();
        this.expiryInMillis = cacheBuilder.getExpireAfter();
        this.maximumSize = cacheBuilder.getMaximumSize();
        if (this.expiryInMillis > 0) {
            Executors.newScheduledThreadPool(2).scheduleAtFixedRate(this::cleanExpired, 0, expiryInMillis / 2, MILLISECONDS);
        }
    }

    public Object get(Object key) {
        lock.lock();
        Optional<ValueWrapper> result = ofNullable(this.cacheMap.get(key));
        lock.unlock();
        return result.map(ValueWrapper::getValue).orElse(null);
    }

    public void put(Object key, Object value) {
        lock.lock();
        StopWatcher started = StopWatcher.createStarted();
        this.cacheMap.put(key, new ValueWrapper(value));
        cacheStatistic.addTimePutting(started.stopAndGet());
        lock.unlock();
    }

    private void cleanExpired() {
        long currentTimeMillis = System.currentTimeMillis();
        cacheMap.entrySet().removeIf(entry -> {
            long lastAccessTime = entry.getValue().getLastAccessTime();
            boolean isExpired = lastAccessTime > 0 && expiryInMillis > 0 && currentTimeMillis > (lastAccessTime + expiryInMillis);
            if (isExpired) {
                log.info("DELETING {}", entry.getValue());
                cacheStatistic.addEviction();
            }
            return isExpired;
        });
    }

    void addEvent(CacheEvent cacheEvent) {
        cacheEvents.add(cacheEvent);
    }


    @Getter
    @Setter
    @ToString
    private static class ValueWrapper {
        private Object value;
        private long lastAccessTime;
        private long accessCount;

        public ValueWrapper(Object value) {
            this.value = value;
        }

        public Object getValue() {
            this.lastAccessTime = System.currentTimeMillis();
            this.accessCount++;
            return value;
        }
    }

    private class LruHashMap extends LinkedHashMap<Object, ValueWrapper> {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            boolean isEldest = size() > maximumSize;
            if (isEldest) {
                log.info("ELDEST {}", eldest.getValue());
                cacheStatistic.addEviction();
            }
            return isEldest;
        }
    }

    private class LfuHashMap extends LinkedHashMap<Object, ValueWrapper> {

        private final Queue<Object> keyAccessQueue = new LinkedBlockingQueue<>();

        @Override
        public ValueWrapper get(Object key) {
            ValueWrapper valueWrapper = super.get(key);
            keyAccessQueue.remove(key);
            keyAccessQueue.add(key);
            return valueWrapper;
        }

        @Override
        public ValueWrapper put(Object key, ValueWrapper value) {
            if (size() + 1 > maximumSize) {
                remove(keyAccessQueue.poll());
                cacheStatistic.addEviction();
            }
            keyAccessQueue.add(key);
            return super.put(key, value);
        }
    }
}
