package com.grainscale.service;

import com.grainscale.dto.StabilizedWeightEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Publica eventos de peso estabilizado no Redis Stream.
 *
 * O Stream funciona como uma fila persistente: os dados ficam
 * armazenados até serem consumidos (diferente do Pub/Sub que perde mensagens).
 */
@Service
public class WeighingPublisher {

    private static final Logger log = LoggerFactory.getLogger(WeighingPublisher.class);
    private static final String STREAM_KEY = "weighing:stabilized";

    private final StringRedisTemplate redisTemplate;

    public WeighingPublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publish(StabilizedWeightEvent event) {
        Map<String, String> fields = new HashMap<>();
        fields.put("scaleId", event.scaleId());
        fields.put("plate", event.plate());
        fields.put("stabilizedWeight", String.valueOf(event.stabilizedWeight()));
        fields.put("readingCount", String.valueOf(event.readingCount()));
        fields.put("timestamp", event.timestamp().toString());

        RecordId recordId = redisTemplate.opsForStream().add(STREAM_KEY, fields);

        log.info("✅ Peso estabilizado publicado no Redis Stream [{}]: placa={}, peso={}kg, leituras={}, recordId={}",
                STREAM_KEY, event.plate(), event.stabilizedWeight(), event.readingCount(), recordId);
    }
}
