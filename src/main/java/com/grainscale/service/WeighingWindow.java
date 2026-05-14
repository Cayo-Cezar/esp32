package com.grainscale.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Janela deslizante que acumula as leituras de peso de um caminhão na balança.
 *
 * Cada vez que o ESP32 envia um POST, uma leitura é adicionada aqui.
 * Quando a janela tem >= 3 segundos de dados E a variância é baixa,
 * o peso é considerado estabilizado.
 */
public class WeighingWindow {

    private final String scaleId;
    private final String plate;
    private final List<Reading> readings = new ArrayList<>();

    public WeighingWindow(String scaleId, String plate) {
        this.scaleId = scaleId;
        this.plate = plate;
    }

    /** Adiciona uma nova leitura de peso */
    public void addReading(double weight) {
        readings.add(new Reading(weight, Instant.now()));
    }

    /** Duração da janela em milissegundos */
    public long getDurationMs() {
        if (readings.size() < 2) return 0;
        var first = readings.getFirst().timestamp();
        var last = readings.getLast().timestamp();
        return last.toEpochMilli() - first.toEpochMilli();
    }

    /** Tempo desde a última leitura em milissegundos */
    public long getIdleMs() {
        if (readings.isEmpty()) return Long.MAX_VALUE;
        return Instant.now().toEpochMilli() - readings.getLast().timestamp().toEpochMilli();
    }

    /** Calcula a variância do peso (mede o quanto oscila) */
    public double getVariance() {
        if (readings.size() < 2) return Double.MAX_VALUE;

        double mean = getMeanWeight();
        double sumSquares = 0;
        for (var r : readings) {
            double diff = r.weight() - mean;
            sumSquares += diff * diff;
        }
        return sumSquares / readings.size();
    }

    /** Calcula a média do peso */
    public double getMeanWeight() {
        return readings.stream()
                .mapToDouble(Reading::weight)
                .average()
                .orElse(0);
    }

    public int getReadingCount() { return readings.size(); }
    public String getScaleId() { return scaleId; }
    public String getPlate() { return plate; }

    /** Uma leitura individual: peso + momento em que foi recebida */
    private record Reading(double weight, Instant timestamp) {}
}
