/**
 * 시계열 데이터 다운샘플링 유틸리티
 * - 대량의 시계열 데이터를 효율적으로 축소
 * - 평균값(AVG) 기반 집계
 */
package com.monito.domains.container.util;

import com.monito.domains.container.dto.response.metrics.TimeSeriesDataDTO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
/**
 작성자: 백승준
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TimeSeriesDownSampler {

    /**
     * 시간 범위에 따라 자동으로 다운샘플링 간격을 결정하고 적용
     * - 모든 시간 범위에서 약 60개 포인트를 목표로 일관된 차트 밀도 유지
     *
     * @param dataPoints 원본 데이터 포인트
     * @param totalMinutes 전체 시간 범위 (분) - 현재 미사용, 하위 호환성 유지
     * @return 다운샘플링된 데이터 포인트
     */
    public static List<TimeSeriesDataDTO> autoDownSample(List<TimeSeriesDataDTO> dataPoints, long totalMinutes) {
        if (dataPoints == null || dataPoints.isEmpty()) {
            return new ArrayList<>();
        }

        int samplingInterval = determineSamplingInterval(dataPoints.size());

        // 샘플링이 필요 없는 경우 (간격이 1 = 원본 그대로)
        if (samplingInterval == 1) {
            return dataPoints;
        }

        return downSample(dataPoints, samplingInterval);
    }

    /**
     * 데이터 포인트 수에 따라 샘플링 간격 결정
     * - 목표: 모든 시간 범위에서 약 60개 포인트 유지
     * - 일관된 차트 밀도로 시각화 품질 향상
     *
     * 예시:
     * - 5분 (60개):       간격 1   → 60개 유지
     * - 10분 (120개):     간격 2   → 60개
     * - 30분 (360개):     간격 6   → 60개
     * - 1시간 (720개):    간격 12  → 60개
     * - 3시간 (2160개):   간격 36  → 60개
     * - 12시간 (8640개):  간격 144 → 60개
     * - 24시간 (17280개): 간격 288 → 60개
     *
     * @param dataPointCount 원본 데이터 포인트 개수
     * @return 샘플링 간격 (몇 개당 1개를 선택할지)
     */
    private static int determineSamplingInterval(int dataPointCount) {
        final int TARGET_POINTS = 60;

        // 원본 데이터가 타겟보다 적거나 같으면 샘플링 불필요
        if (dataPointCount <= TARGET_POINTS) {
            return 1;
        }

        // 타겟 포인트 수에 맞춰 간격 계산
        // 예: 360개 데이터 → 360/60 = 6 → 6개당 1개 선택 → 결과 60개
        return Math.max(1, dataPointCount / TARGET_POINTS);
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
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long count = chunk.stream()
                .map(TimeSeriesDataDTO::getValue)
                .filter(Objects::nonNull)
                .count();

        if (count == 0) {
            return BigDecimal.ZERO;
        }

        return sum.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
    }
}