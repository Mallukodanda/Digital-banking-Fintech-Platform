package com.fintech.bank.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisCacheConfig Unit Tests")
class RedisCacheConfigTest {

    @Mock
    private RedisConnectionFactory connectionFactory;

    private final RedisCacheConfig redisCacheConfig = new RedisCacheConfig();

    @Test
    @DisplayName("Should configure RedisTemplate with String key serializer and JSON value serializer")
    void shouldConfigureRedisTemplate() {
        RedisTemplate<String, Object> template = redisCacheConfig.redisTemplate(connectionFactory);

        assertThat(template).isNotNull();
        assertThat(template.getKeySerializer()).isNotNull();
        assertThat(template.getValueSerializer()).isNotNull();
    }

    @Test
    @DisplayName("Should configure CacheManager with pre-configured domain cache spaces")
    void shouldConfigureCacheManagerWithDomainCaches() {
        CacheManager cacheManager = redisCacheConfig.cacheManager(connectionFactory);

        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager.getCacheNames()).contains(
                CacheNames.CUSTOMER_PROFILES,
                CacheNames.BENEFICIARIES,
                CacheNames.SYSTEM_CONFIGS
        );
    }
}
