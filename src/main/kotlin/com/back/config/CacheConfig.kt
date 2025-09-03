package com.back.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun cacheManager(redisConnectionFactory: RedisConnectionFactory): CacheManager {
        val configuration = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30)) // 기본 TTL
            .disableCachingNullValues() // null 값 캐싱 방지
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())) // 키 직렬화 캐시 키를 Redis에 문자열로 저장
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                GenericJackson2JsonRedisSerializer()
            )) // 캐시 값을 JSON 형태로 저장 객체를 JSON으로 변환하여 저장

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(configuration)
            .withCacheConfiguration("members",
                configuration.entryTtl(Duration.ofHours(1))) // 개별 TTL 설정
            .build()
    }
}
