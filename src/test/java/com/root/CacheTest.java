package com.root;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
class CacheTest {

    @Test
    void create_lfuCache_successful() {
        Cache cache = CacheBuilder.lfu()
                .maximumSize(3)
                .removalListener(n -> log.info("was removed {}", n))
                .build();
        cache.put(1, 1);
        cache.put(2, 2);
        cache.get(1);
        cache.get(2);
        cache.get(2);
        cache.get(1);
        cache.get(1);
        cache.put(3, 3);
        cache.get(3);
        cache.get(3);
        cache.get(3);
        cache.put(4, 4);
        assertThat(cache.get(1)).isEqualTo(1);
        assertThat(cache.get(2)).isNull();
        assertThat(cache.get(3)).isEqualTo(3);
        assertThat(cache.get(4)).isEqualTo(4);
    }

    @Test
    void create_lruCache_successful() {
        Cache cache = CacheBuilder.lru()
                .maximumSize(2)
                .removalListener(n -> log.info("was removed {}", n))
                .build();
        cache.put(1, 1);
        cache.put(2, 2);
        cache.get(1);
        cache.get(1);
        cache.get(1);
        cache.put(3, 3);
        assertThat(cache.get(1)).isNull();
        assertThat(cache.get(2)).isEqualTo(2);
        assertThat(cache.get(3)).isEqualTo(3);
    }

    @Test
    void cacheExpireAfter_successful() {
        RemovalListener mockListener = mock(RemovalListener.class);
        doNothing().when(mockListener).onRemoval(any());

        Cache cache = CacheBuilder.lru()
                .maximumSize(3)
                .removalListener(n -> log.info("was removed {}", n))
                .removalListener(mockListener)
                .expireAfterAccess(2, SECONDS)
                .build();
        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(3, 3);
        assertThat(cache.get(1)).isEqualTo(1);
        assertThat(cache.get(2)).isEqualTo(2);
        await().pollDelay(3000, MILLISECONDS).untilAsserted(() -> assertThat(cache.get(1)).isNull());
        await().pollDelay(3000, MILLISECONDS).untilAsserted(() -> assertThat(cache.get(2)).isNull());
        assertThat(cache.get(3)).isEqualTo(3);
        await().pollDelay(3000, MILLISECONDS).untilAsserted(() -> assertThat(cache.get(3)).isNull());

        verify(mockListener, times(3)).onRemoval(any());
    }

    @Test
    void removalListener_test() {
        RemovalListener removalListener = mock(RemovalListener.class);
        doNothing().when(removalListener).onRemoval(any());

        Cache cache = CacheBuilder.lru()
                .maximumSize(1)
                .expireAfterAccess(2000, MILLISECONDS)
                .removalListener(removalListener)
                .build();
        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(3, 3);

        verify(removalListener, times(2)).onRemoval(any());
    }

    @RepeatedTest(20)
    void verify_statistic() {
        EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandom();
        Cache cache = CacheBuilder.lfu()
                .maximumSize(450)
                .build();
        random.objects(TestObj.class, 500).forEach(o -> cache.put(o, o));
        CacheStatistic actual = cache.getCacheStatistic();

        CacheStatistic expected = new CacheStatistic();
        expected.setEvictionsCount(50);
        expected.setAddCount(500);
        log.info("Average Time Putting {}", actual.getAverageTimePutting());
        assertThat(actual).usingRecursiveComparison().ignoringFields("averageTimePutting").isEqualTo(expected);
        assertThat(actual.getAverageTimePutting().toNanos()).isNotNegative();
        assertThat(actual.getAverageTimePutting()).isLessThan(Duration.of(1, ChronoUnit.MILLIS));
    }


    @RequiredArgsConstructor
    @Getter
    @ToString
    public static class TestObj {
        private final String testField;
    }
}
