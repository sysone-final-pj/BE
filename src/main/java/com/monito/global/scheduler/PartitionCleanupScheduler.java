package com.monito.global.scheduler;

import java.time.LocalDate;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 파티셔닝된 로그 테이블의 오래된 파티션을 자동으로 삭제하는 스케줄러
 * - 보관 기간: application.yml에서 설정 (기본 30일)
 * - 실행 시간: application.yml에서 설정 (기본 매일 새벽 2시)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PartitionCleanupScheduler {

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.scheduler.metrics-partition-cleanup.retention-days:30}")
    private int metricsRetentionDays;

    @Value("${app.scheduler.oom-partition-cleanup.retention-days:180}")
    private int oomRetentionDays;

    /**
     * 설정된 시간에 오래된 파티션 삭제
     * - Cron 표현식: application.yml에서 설정
     * - 전용 스레드 풀(partitionCleanupExecutor) 사용
     */
    @Async("partitionCleanupExecutor")
    @Scheduled(cron = "${app.scheduler.metrics-partition-cleanup.cron:0 0 2 * * *}")
    public void cleanupOldMetricsPartitions() {
        log.info("파티션 정리 작업 시작 - 보관 기간: {}일", metricsRetentionDays);

        LocalDate cutoffDate = LocalDate.now().minusDays(metricsRetentionDays);

        try {
            // Container Logs 파티션 삭제
            dropPartitionsForTable(jdbcTemplate, "container_logs", cutoffDate);

            // Container Stats Logs 파티션 삭제
            dropPartitionsForTable(jdbcTemplate, "container_stats_logs", cutoffDate);

            log.info("파티션 정리 작업 완료");
        } catch (Exception e) {
            log.error("파티션 정리 작업 실패", e);
        }
    }

    /**
     * 설정된 시간에 오래된 파티션 삭제
     * - Cron 표현식: application.yml에서 설정
     * - 전용 스레드 풀(partitionCleanupExecutor) 사용
     */
    @Async("partitionCleanupExecutor")
    @Scheduled(cron = "${app.scheduler.oom-partition-cleanup.cron:0 0 3 * * *}")
    public void cleanupOldOOMPartitions() {
        log.info("OOM 파티션 정리 작업 시작 - 보관 기간: {}일", oomRetentionDays);

        LocalDate cutoffDate = LocalDate.now().minusDays(oomRetentionDays);

        try {
            dropPartitionsForTable(jdbcTemplate, "oom_events", cutoffDate);

            log.info("OOM 파티션 정리 작업 완료");
        } catch (Exception e) {
            log.error("OOM 파티션 정리 작업 실패", e);
        }
    }

    /**
     * 특정 테이블의 오래된 파티션 삭제
     */
    private void dropPartitionsForTable(JdbcTemplate jdbcTemplate, String tableName, LocalDate cutoffDate) {
        // Oracle의 경우 파티션 이름 조회 후 삭제
        String queryPartitions = """
            SELECT partition_name
            FROM user_tab_partitions
            WHERE table_name = UPPER(?)
            AND partition_name != 'P_INITIAL'
            ORDER BY partition_position
        """;

        try {
            jdbcTemplate.query(queryPartitions, rs -> {
                String partitionName = rs.getString("partition_name");

                // 파티션 이름에서 날짜 추출 (예: SYS_P12345 형식은 건너뛰고, 날짜 기반 파티션만 처리)
                // Oracle INTERVAL 파티셔닝은 시스템 생성 파티션 이름 사용
                // 실제 파티션의 HIGH_VALUE로 판단해야 함
                try {
                    dropPartitionByHighValue(jdbcTemplate, tableName, partitionName, cutoffDate);
                } catch (Exception e) {
                    log.warn("파티션 삭제 실패: {}.{}", tableName, partitionName, e);
                }
            }, tableName.toUpperCase());

        } catch (Exception e) {
            log.error("테이블 {} 파티션 조회 실패", tableName, e);
        }
    }

    /**
     * 파티션의 HIGH_VALUE를 확인하고 cutoff date 이전이면 삭제
     */
    private void dropPartitionByHighValue(JdbcTemplate jdbcTemplate, String tableName,
                                         String partitionName, LocalDate cutoffDate) {
        String queryHighValue = """
            SELECT high_value
            FROM user_tab_partitions
            WHERE table_name = ? AND partition_name = ?
        """;

        try {
            String highValue = jdbcTemplate.queryForObject(
                queryHighValue,
                String.class,
                tableName.toUpperCase(),
                partitionName
            );

            // HIGH_VALUE는 TO_DATE 함수 형식으로 저장됨
            // 예: TO_DATE(' 2025-01-02 00:00:00', 'SYYYY-MM-DD HH24:MI:SS', 'NLS_CALENDAR=GREGORIAN')
            // 간단한 날짜 파싱 (실제로는 더 정교한 파싱 필요)
            if (highValue != null && isPartitionOlderThanCutoff(highValue, cutoffDate)) {
                dropPartition(jdbcTemplate, tableName, partitionName);
            }
        } catch (Exception e) {
            log.debug("파티션 {} HIGH_VALUE 확인 실패 (건너뜀)", partitionName);
        }
    }

    /**
     * HIGH_VALUE 문자열에서 날짜를 추출하고 cutoff date와 비교
     */
    private boolean isPartitionOlderThanCutoff(String highValue, LocalDate cutoffDate) {
        try {
            // HIGH_VALUE에서 날짜 부분 추출 (예: ' 2025-01-02 00:00:00')
            int startIdx = highValue.indexOf("'") + 1;
            int endIdx = highValue.indexOf("'", startIdx);
            if (startIdx > 0 && endIdx > startIdx) {
                String dateStr = highValue.substring(startIdx, endIdx).trim().substring(0, 10);
                LocalDate partitionDate = LocalDate.parse(dateStr);
                return partitionDate.isBefore(cutoffDate);
            }
        } catch (Exception e) {
            log.debug("HIGH_VALUE 파싱 실패: {}", highValue);
        }
        return false;
    }

    /**
     * 파티션 삭제 실행
     */
    private void dropPartition(JdbcTemplate jdbcTemplate, String tableName, String partitionName) {
        String dropSql = String.format(
            "ALTER TABLE %s DROP PARTITION %s UPDATE INDEXES",
            tableName,
            partitionName
        );

        try {
            jdbcTemplate.execute(dropSql);
            log.info("파티션 삭제 성공: {}.{}", tableName, partitionName);
        } catch (Exception e) {
            log.error("파티션 삭제 실패: {}.{}", tableName, partitionName, e);
            throw e;
        }
    }
}