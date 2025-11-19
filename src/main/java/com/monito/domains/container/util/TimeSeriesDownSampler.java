package com.monito.domains.container.util;

import com.monito.domains.container.dto.response.metrics.TimeSeriesDataDTO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 시계열 데이터 다운샘플링 유틸리티
 * - 대량의 시계열 데이터를 효율적으로 축소
 * - 평균값(AVG) 기반 집계
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TimeSeriesDownSampler {

    /**
     * 시간 범위에 따라 자동으로 다운샘플링 간격을 결정하고 적용
     *
     * @param dataPoints 원본 데이터 포인트
     * @param totalMinutes 전체 시간 범위 (분)
     * @return 다운샘플링된 데이터 포인트
     */
    public static List<TimeSeriesDataDTO> autoDownSample(List<TimeSeriesDataDTO> dataPoints, long totalMinutes) {
        if (dataPoints == null || dataPoints.isEmpty()) {
            return new ArrayList<>();
        }

        int samplingInterval = determineSamplingInterval(totalMinutes);

        // 샘플링이 필요 없는 경우 (간격이 1 = 원본 그대로)
        if (samplingInterval == 1) {
            return dataPoints;
        }

        return downSample(dataPoints, samplingInterval);
    }

    /**
     * 시간 범위에 따라 샘플링 간격 결정
     * - 1-5분: 샘플링 없음 (원본 5초 간격) -> ~60 포인트
     * - 10-30분: 30초 간격 (6개당 1개) -> 20-60 포인트
     * - 1-3시간: 2분 간격 (24개당 1개) -> 30-90 포인트
     * - 6-12시간: 5분 간격 (60개당 1개) -> 72-144 포인트
     * - 24시간: 10분 간격 (120개당 1개) -> ~144 포인트
     *
     * @param totalMinutes 전체 시간 범위 (분)
     * @return 샘플링 간격 (몇 개당 1개를 선택할지)
     */
    private static int determineSamplingInterval(long totalMinutes) {
        if (totalMinutes <= 5) {
            return 1;      // 샘플링 없음
        } else if (totalMinutes <= 30) {
            return 6;      // 30초 간격 (5초 * 6 = 30초)
        } else if (totalMinutes <= 180) {
            return 24;     // 2분 간격 (5초 * 24 = 120초)
        } else if (totalMinutes <= 720) {
            return 60;     // 5분 간격 (5초 * 60 = 300초)
        } else {
            return 120;    // 10분 간격 (5초 * 120 = 600초)
        }
    }

    /**
     * 지정된 간격으로 데이터 다운샘플링 (평균값 기반)
     *
     * @param dataPoints 원본 데이터 포인트
     * @param interval 샘플링 간격 (N개당 1개)
     * @return 다운샘플링된 데이터 포인트
     */
    private static List<TimeSeriesDataDTO> downSample(List<TimeSeriesDataDTO> dataPoints, int interval) {
        List<TimeSeriesDataDTO> sampledData = new ArrayList<>();

        for (int i = 0; i < dataPoints.size(); i += interval) {
            // 구간의 끝 인덱스 계산
            int endIdx = Math.min(i + interval, dataPoints.size());

            // 구간 내 데이터 추출
            List<TimeSeriesDataDTO> chunk = dataPoints.subList(i, endIdx);

            // 평균값 계산
            BigDecimal avgValue = calculateAverage(chunk);

            // 구간의 중간 시간 사용 (또는 첫 번째 시간)
            LocalDateTime timestamp = chunk.get(chunk.size() / 2).getTimestamp();

            sampledData.add(TimeSeriesDataDTO.from(timestamp, avgValue));
        }

        return sampledData;
    }

    /**
     * 데이터 청크의 평균값 계산
     */
    private static BigDecimal calculateAverage(List<TimeSeriesDataDTO> chunk) {
        if (chunk.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = chunk.stream()
                .map(TimeSeriesDataDTO::getValue)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long count = chunk.stream()
                .map(TimeSeriesDataDTO::getValue)
                .filter(value -> value != null)
                .count();

        if (count == 0) {
            return BigDecimal.ZERO;
        }

        return sum.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
    }
}