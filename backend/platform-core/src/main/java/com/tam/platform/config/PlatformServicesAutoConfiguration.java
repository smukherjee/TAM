package com.tam.platform.config;

import com.tam.platform.cache.CacheService;
import com.tam.platform.cache.RedisCacheService;
import com.tam.platform.context.ContextAwareTaskDecorator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@AutoConfiguration
@EntityScan(basePackages = "com.tam.platform")
@EnableJpaRepositories(basePackages = "com.tam.platform")
public class PlatformServicesAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "platformRedisTemplate")
    public RedisTemplate<String, Object> platformRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    @ConditionalOnMissingBean(CacheService.class)
    public CacheService cacheService(RedisTemplate<String, Object> platformRedisTemplate) {
        return new RedisCacheService(platformRedisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(ConfigurationService.class)
    public ConfigurationService configurationService(TenantConfigurationRepository repository) {
        return new DatabaseConfigurationService(repository);
    }

    @Bean
    @ConditionalOnMissingBean(Executor.class)
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("PlatformAsync-");
        executor.setTaskDecorator(new ContextAwareTaskDecorator());
        executor.initialize();
        return executor;
    }
}
