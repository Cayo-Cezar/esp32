package com.grainscale.dto;

import java.time.Instant;

/**
 * Evento publicado no Redis Stream quando o peso é estabilizado.
 * 
 */
public record StabilizedWeightEvent(
                String scaleId,
                String plate,
                Double stabilizedWeight, // média do peso estabilizado (kg)
                int readingCount, // quantas leituras foram usadas
                Instant timestamp) {
}
