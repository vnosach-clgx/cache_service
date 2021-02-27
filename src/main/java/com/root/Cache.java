package com.root;

import lombok.Getter;
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

    private final List<RemovalListener> removalListeners;
    private final long expiryInMillis;
    private final long maximumSize;

    protected Cache(CacheBuilder cacheBuilder) {
        this.cacheMap = cacheBuilder.getCacheType().equals(LRU) ? new LruHashMap() : new LfuHashMap();
        this.expiryInMillis = cacheBuilder.getExpireAfter();
        this.maximumSize = cacheBuilder.getMaximumSize();
        this.removalListeners = cacheBuilder.getRemovalListeners();
        this.removalListeners.add(n -> cacheStatistic.addEviction());
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
            boolean isExpired = lastAccessTime > 0 && currentTimeMillis > (lastAccessTime + expiryInMillis);
            if (isExpired) {
                notifyRemovalListeners(Map.entry(entry.getKey(), entry.getValue().getValue()));
            }
            return isExpired;
        });
    }

    private void notifyRemovalListeners(Map.Entry<Object, Object> entry) {
        removalListeners.forEach(removalListener -> removalListener.onRemoval(entry));
    }

    private class LruHashMap extends LinkedHashMap<Object, ValueWrapper> {

        @Override
        public ValueWrapper remove(Object key) {
            Object value = super.get(key).getValue();
            notifyRemovalListeners(Map.entry(key, value));
            return super.remove(key);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            boolean isEldest = size() > maximumSize;
            if (isEldest) {
                notifyRemovalListeners(eldest);
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
                notifyRemovalListeners(Map.entry(key, value.getValue()));
                remove(keyAccessQueue.poll());
            }
            keyAccessQueue.add(key);
            return super.put(key, value);
        }
    }
}
