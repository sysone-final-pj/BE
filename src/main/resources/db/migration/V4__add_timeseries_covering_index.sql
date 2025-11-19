-- 시계열 데이터 조회 최적화를 위한 Covering Index
-- Index-Only Scan을 가능하게 하여 테이블 접근을 최소화
-- 압축 옵션으로 인덱스 크기 절감

-- 1. 시계열 데이터 전용 복합 인덱스 (Covering Index)
-- container_id + collected_at로 파티션 프루닝 + 필요한 모든 컬럼 포함
CREATE INDEX IDX_CONTAINER_STATS_TIMESERIES
ON container_stats_logs(
    container_id,         -- WHERE 조건
    collected_at,         -- WHERE 조건 + ORDER BY + 파티션 키
    cpu_percent,          -- CPU 사용률 조회용
    mem_percent,          -- 메모리 사용률 조회용
    rx_bytes_per_sec,     -- 네트워크 수신 속도 조회용
    tx_bytes_per_sec,     -- 네트워크 송신 속도 조회용
    rx_pps,               -- 네트워크 수신 패킷 레이트
    tx_pps                -- 네트워크 송신 패킷 레이트
)
LOCAL                     -- 로컬 파티션 인덱스 (각 파티션마다 독립적)
COMPRESS;

-- 2. container_hash 기반 조회용 인덱스 (기존에 없던 인덱스)
-- findLatestByContainerHash 최적화
CREATE INDEX IDX_CONTAINER_STATS_HASH_COLLECTED
ON container_stats_logs(
    container_hash,
    collected_at DESC     -- 최신 데이터 조회 최적화
)
LOCAL
COMPRESS;