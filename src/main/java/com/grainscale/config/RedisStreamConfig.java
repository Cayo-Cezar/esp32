package com.grainscale.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Cria o consumer group do Redis Stream no startup da aplicação.
 *
 * O consumer group permite que múltiplas instâncias do worker
 * leiam do mesmo stream sem duplicar o processamento.
 */
@Configuration
public class RedisStreamConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisStreamConfig.class);
    public static final String STREAM_KEY = "weighing:stabilized";
    public static final String GROUP_NAME = "grain-scale-group";

    @Bean
    public CommandLineRunner createConsumerGroup(StringRedisTemplate redisTemplate) {
        return args -> {
            try {
                redisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0"), GROUP_NAME);
                log.info("Consumer group '{}' criado no stream '{}'", GROUP_NAME, STREAM_KEY);
            } catch (Exception e) {
                // Grupo já existe ou stream não existe ainda — normal no primeiro startup
                log.info("Consumer group '{}' já existe ou stream será criado na primeira publicação", GROUP_NAME);
            }
        };
    }
}
