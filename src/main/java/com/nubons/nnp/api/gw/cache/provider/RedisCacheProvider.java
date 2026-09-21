package com.nubons.nnp.api.gw.cache.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@ConditionalOnProperty(name = "gateway.cache.provider", havingValue = "redis")
public class RedisCacheProvider implements CacheProvider {
    private final RedisTemplate redisTemplate;
    @Value("${gateway.cache.ttl:30}")
    private long cacheTtlMinutes;


	public RedisCacheProvider(/* RedisTemplate redisTemplate */ RedisConnectionFactory rcf) {
        //this.redisTemplate = redisTemplate;
        this.redisTemplate = new RedisTemplate<String, String>();
        this.redisTemplate.setConnectionFactory(rcf);
        this.redisTemplate.setKeySerializer(new StringRedisSerializer());
        this.redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        this.redisTemplate.afterPropertiesSet();
    }

    @Override
    public void put(String key, Object value) {
        log.debug("redis cache put is called");
        redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(cacheTtlMinutes));
    }

    @Override
    public void put(String key, Object value, long ttl) {
        log.debug("redis cache put is called with ttl: {}", ttl);
        // Set default ttl if it is 0 somehow
        if (ttl == 0) {
            ttl = cacheTtlMinutes;
        }
        redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(ttl));
    }

    @Override
    public Object get(String key) {
        log.debug("redis cache get is called");
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public <T> T get(String key, Class<T> cls) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    @Override
    public void evict(String key) {
        log.debug("redis cache evict is called");
        redisTemplate.unlink(key);
    }
}
