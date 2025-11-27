package com.monito.domains.container.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
/**
 작성자: 백승준
 */
@Component
public class CpuMetricsCalculator {

    private static final int SCALE = 4;

    /**
     * 평균 계산
     */
    public BigDecimal avg(List<BigDecimal> samples) {
        if (samples == null || samples.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = samples.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(samples.size()), SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 최근 N개 기준 평균 (N개 미만일 경우 전체 평균)
     */
    public BigDecimal avgLast(List<BigDecimal> samples, int n) {
        if (samples == null || samples.isEmpty()) {
            return BigDecimal.ZERO;
        }

        int size = samples.size();
        if (size <= n) {
            return avg(samples);
        }

        List<BigDecimal> sub = samples.subList(size - n, size);
        return avg(sub);
    }

    /**
     * 퍼센타일 계산 (예: p=0.95 → 95th percentile)
     */
    public BigDecimal percentile(List<BigDecimal> samples, double p) {
        if (samples == null || samples.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<BigDecimal> sorted = samples.stream()
                .sorted(Comparator.naturalOrder())
                .toList();

        int index = (int) Math.ceil(p * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));

        return sorted.get(index).setScale(SCALE, RoundingMode.HALF_UP);
    }
}
