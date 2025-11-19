-- Container Logs source 컬럼 인덱스 추가
-- 목적: logSource 필터링 성능 최적화 (대시보드 STDOUT/STDERR 카운트 쿼리 등)
-- 영향받는 쿼리:
--   1. DashboardServiceImpl - countBySourceAndLoggedAtBetween()
--   2. ContainerLogRepository - findLogs() with logSource filter

-- source 컬럼 단일 인덱스 (로컬 파티션 인덱스)
-- 파티션 프루닝과 함께 사용하면 최적의 성능 발휘
CREATE INDEX IDX_CONTAINER_LOG_SOURCE
ON container_logs(source)
LOCAL;

-- source + logged_at 복합 인덱스 (로컬 파티션 인덱스)
-- 시간 범위 + source 필터링을 동시에 사용하는 쿼리 최적화
-- 예: WHERE source = 'STDOUT' AND logged_at >= ? AND logged_at < ?
CREATE INDEX IDX_CONTAINER_LOG_SOURCE_LOGGED_AT
ON container_logs(source, logged_at)
LOCAL
COMPRESS;

-- container_id + source + logged_at 복합 인덱스 (로컬 파티션 인덱스)
-- 특정 컨테이너의 특정 소스 로그 조회 최적화
-- 예: WHERE container_id = ? AND source = 'STDERR' AND logged_at >= ?
CREATE INDEX IDX_CONTAINER_LOG_CONTAINER_SOURCE_LOGGED_AT
ON container_logs(container_id, source, logged_at)
LOCAL
COMPRESS;

-- 인덱스 생성 후 통계 수집 (Oracle Optimizer 최적화)
BEGIN
    DBMS_STATS.GATHER_TABLE_STATS(
        ownname => USER,
        tabname => 'CONTAINER_LOGS',
        cascade => TRUE,
        degree => 4
    );
END;
/
