package com.root;

import com.root.util.StopWatcher;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.root.CacheBuilder.CACHE_TYPE.LRU;
import static java.lang.System.currentTimeMillis;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
public class Cache {

    private final Map<Object, ValueWrapper> cacheStorage;
    @Getter
    private final CacheStatistic cacheStatistic = new CacheStatistic();
    private final Lock lock = new ReentrantLock();

    private final List<RemovalListener> removalListeners;
    private final long expiryInMillis;
    private final long maximumSize;

    protected Cache(CacheBuilder cacheBuilder) {
        this.cacheStorage = LRU.equals(cacheBuilder.getCacheType()) ? new LruHashMap() : new LfuHashMap();
        this.expiryInMillis = cacheBuilder.getExpireAfter();
        this.maximumSize = cacheBuilder.getMaximumSize();
        this.removalListeners = cacheBuilder.getRemovalListeners();
        this.removalListeners.add(n -> cacheStatistic.increaseEvictionCount());
        if (this.expiryInMillis > 0) {
            Executors.newScheduledThreadPool(2).scheduleAtFixedRate(this::cleanExpired, 0, expiryInMillis / 2, MILLISECONDS);
        }
    }

    public Object get(Object key) {
        lock.lock();
        Object result = ofNullable(this.cacheStorage.get(key)).map(ValueWrapper::unwrap).orElse(null);
        lock.unlock();
        return result;
    }

    public void put(Object key, Object value) {
        lock.lock();
        StopWatcher started = StopWatcher.createStarted();
        this.cacheStorage.put(key, new ValueWrapper(value));
        cacheStatistic.puttingDuration(started.stopAndGet());
        lock.unlock();
    }

    private void cleanExpired() {
        cacheStorage.entrySet().removeIf(entry -> {
            long lastAccessTime = entry.getValue().getLastAccessTime();
            boolean isExpired = lastAccessTime > 0 && currentTimeMillis() > (lastAccessTime + expiryInMillis);
            if (isExpired) {
                notifyRemovalListeners(Map.entry(entry.getKey(), entry.getValue().unwrap()));
            }
            return isExpired;
        });
    }

    private void notifyRemovalListeners(Map.Entry<Object, Object> entry) {
        removalListeners.forEach(removalListener -> removalListener.onRemoval(entry));
    }

    private class LruHashMap extends LinkedHashMap<Object, ValueWrapper> {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Object, ValueWrapper> eldest) {
            boolean isEldest = size() > maximumSize;
            if (isEldest) {
                notifyRemovalListeners(Map.entry(eldest.getKey(), eldest.getValue().unwrap()));
            }
            return isEldest;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    private class LfuHashMap extends LinkedHashMap<Object, ValueWrapper> {

        private final Comparator<PriorityKey> reversed = Comparator.comparing(PriorityKey::getAccessCounter)
                .thenComparing(PriorityKey::hashCode)
                .reversed();

        private final TreeSet<PriorityKey> keyAccessQueue= new TreeSet<>(reversed);

        @Override
        public ValueWrapper get(Object key) {
            ValueWrapper valueWrapper = super.get(key);
            ofNullable(valueWrapper).map(ValueWrapper::getAccessCounter)
                    .ifPresent(accessCounter -> {
                        keyAccessQueue.remove(new PriorityKey(accessCounter, key));
                        keyAccessQueue.add(new PriorityKey(accessCounter + 1, key));
                    });

            return valueWrapper;
        }

        @Override
        public ValueWrapper put(Object key, ValueWrapper value) {
            if (size() + 1 > maximumSize) {
                PriorityKey rarelyRequestedKey = keyAccessQueue.pollLast();
                notifyRemovalListeners(Map.entry(rarelyRequestedKey.getKey(), super.get(rarelyRequestedKey.getKey()).unwrap()));
                remove(rarelyRequestedKey.getKey());
            }
            keyAccessQueue.add(new PriorityKey(0L, key));
            return super.put(key, value);
        }

        @Getter
        @EqualsAndHashCode(exclude = "accessCounter")
        @AllArgsConstructor
        private class PriorityKey {
            private final Long accessCounter;
            private final Object key;
        }
    }
}
