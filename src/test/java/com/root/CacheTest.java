package com.root;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
class CacheTest {

    @Test
    void create_lfuCache_successful() {
        Cache cache = CacheBuilder.ofLfu()
                .maximumSize(3)
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
        Cache cache = CacheBuilder.ofLru()
                .maximumSize(2)
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
    void cacheExpireAfter_successful() throws InterruptedException {
        Cache cache = CacheBuilder.ofLru()
                .maximumSize(3)
                .expireAfterAccess(2000)
                .build();
        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(3, 3);
        assertThat(cache.get(1)).isEqualTo(1);
        assertThat(cache.get(2)).isEqualTo(2);
        Thread.sleep(3100);
        assertThat(cache.get(1)).isNull();
        assertThat(cache.get(2)).isNull();
        assertThat(cache.get(3)).isEqualTo(3);
        Thread.sleep(3100);
        assertThat(cache.get(3)).isNull();
    }

    @Test
    void removalListener_test() {
        RemovalListener removalListener = mock(RemovalListener.class);
        doNothing().when(removalListener).onRemoval(any());

        Cache cache = CacheBuilder.ofLru()
                .maximumSize(1)
                .expireAfterAccess(2000)
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
        Cache cache = CacheBuilder.ofLfu()
                .maximumSize(450)
                .build();
        random.objects(TestObj.class, 500).forEach(o -> cache.put(o, o));
        CacheStatistic actual = cache.getCacheStatistic();

        CacheStatistic expected = new CacheStatistic();
        expected.setEvictionsCount(50);
        expected.setAddCount(500);
        log.info("Average Time Putting {}", actual.getAverageTimePutting());
        assertThat(actual).usingRecursiveComparison().ignoringFields("averageTimePutting").isEqualTo(expected);
        assertThat(actual.getAverageTimePutting()).isNotNegative();
        assertThat(actual.getAverageTimePutting()).isLessThan(20);
    }


    @RequiredArgsConstructor
    @Getter
    public static class TestObj {
        private final String testField;
    }
}
