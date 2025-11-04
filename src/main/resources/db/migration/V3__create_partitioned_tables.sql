-- Container Logs 테이블 (일별 파티셔닝)
CREATE TABLE container_logs (
    id NUMBER NOT NULL,
    container_id NUMBER NOT NULL,
    log_message CLOB NOT NULL,
    source VARCHAR2(20),
    logged_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT PK_CONTAINER_LOGS PRIMARY KEY (id, logged_at),
    CONSTRAINT FK_CONTAINER_LOG_CONTAINER FOREIGN KEY (container_id) REFERENCES containers(id)
)
PARTITION BY RANGE (logged_at)
INTERVAL (NUMTODSINTERVAL(1, 'DAY'))
(
    PARTITION p_initial VALUES LESS THAN (TO_DATE('2025-01-01', 'YYYY-MM-DD'))
)
ENABLE ROW MOVEMENT;

-- Container Logs 인덱스 (로컬 파티션 인덱스)
CREATE INDEX IDX_CONTAINER_LOG_LOGGED_AT ON container_logs(logged_at) LOCAL;
CREATE INDEX IDX_CONTAINER_LOG_CONTAINER_LOGGED_AT ON container_logs(container_id, logged_at) LOCAL;

-- Container Stats Logs 테이블 (일별 파티셔닝)
CREATE TABLE container_stats_logs (
    id NUMBER NOT NULL,
    container_id NUMBER NOT NULL,
    container_hash VARCHAR2(64) NOT NULL,
    state VARCHAR2(20) NOT NULL,
    health VARCHAR2(20) NOT NULL,

    -- CPU 관련
    cpu_percent NUMBER(6,2),
    cpu_core_usage NUMBER(6,2),
    host_cpu_usage_total NUMBER NOT NULL,
    cpu_usage_total NUMBER NOT NULL,
    cpu_user NUMBER NOT NULL,
    cpu_system NUMBER NOT NULL,
    cpu_quota NUMBER NOT NULL,
    cpu_period NUMBER NOT NULL,
    online_cpus NUMBER NOT NULL,
    throttling_periods NUMBER NOT NULL,
    throttled_periods NUMBER NOT NULL,
    throttled_time NUMBER NOT NULL,

    -- Memory 관련
    mem_percent NUMBER(6,2) NOT NULL,
    mem_usage NUMBER NOT NULL,
    mem_max_usage NUMBER NOT NULL,

    -- Block I/O 관련
    blk_read NUMBER NOT NULL,
    blk_write NUMBER NOT NULL,
    blk_read_per_sec NUMBER NOT NULL,
    blk_write_per_sec NUMBER NOT NULL,

    -- Network 관련
    rx_bytes NUMBER NOT NULL,
    tx_bytes NUMBER NOT NULL,
    rx_packets NUMBER NOT NULL,
    tx_packets NUMBER NOT NULL,
    network_total_bytes NUMBER NOT NULL,
    rx_bytes_per_sec NUMBER NOT NULL,
    tx_bytes_per_sec NUMBER NOT NULL,
    rx_pps NUMBER NOT NULL,
    tx_pps NUMBER NOT NULL,
    rx_failure_rate NUMBER(6,2),
    tx_failure_rate NUMBER(6,2),
    rx_errors NUMBER NOT NULL,
    tx_errors NUMBER NOT NULL,
    rx_dropped NUMBER NOT NULL,
    tx_dropped NUMBER NOT NULL,

    -- 파일시스템 관련
    size_rw NUMBER NOT NULL,
    size_root_fs NUMBER NOT NULL,

    -- 시간 정보
    collected_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,

    CONSTRAINT PK_CONTAINER_STATS_LOGS PRIMARY KEY (id, collected_at),
    CONSTRAINT FK_CONTAINER_STATS_LOG_CONTAINER FOREIGN KEY (container_id) REFERENCES containers(id)
)
PARTITION BY RANGE (collected_at)
INTERVAL (NUMTODSINTERVAL(1, 'DAY'))
(
    PARTITION p_initial VALUES LESS THAN (TO_DATE('2025-01-01', 'YYYY-MM-DD'))
)
ENABLE ROW MOVEMENT;

-- Container Stats Logs 인덱스 (로컬 파티션 인덱스)
CREATE INDEX IDX_CONTAINER_STATS_COLLECTED_AT ON container_stats_logs(collected_at) LOCAL;
CREATE INDEX IDX_CONTAINER_STATS_CONTAINER_COLLECTED_AT ON container_stats_logs(container_id, collected_at) LOCAL;

-- OOM Events 테이블 (7일 단위 파티셔닝)
CREATE TABLE oom_events (
    id NUMBER NOT NULL,
    container_id NUMBER NOT NULL,
    container_hash VARCHAR2(64) NOT NULL,
    container_name VARCHAR2(50) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    agent_key VARCHAR2(255),
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT PK_OOM_EVENTS PRIMARY KEY (id, occurred_at),
    CONSTRAINT FK_OOM_CONTAINER FOREIGN KEY (container_id) REFERENCES containers(id)
)
PARTITION BY RANGE (occurred_at)
INTERVAL (NUMTODSINTERVAL(7, 'DAY'))
(
    PARTITION p_oom_initial VALUES LESS THAN (TO_DATE('2025-01-01', 'YYYY-MM-DD'))
)
ENABLE ROW MOVEMENT;

-- OOM Events 인덱스 (로컬 파티션 인덱스)
CREATE INDEX IDX_OOM_OCCURRED_AT ON oom_events(occurred_at) LOCAL;
CREATE INDEX IDX_OOM_CONTAINER_OCCURRED_AT ON oom_events(container_id, occurred_at) LOCAL;
CREATE INDEX IDX_OOM_CONTAINER_HASH ON oom_events(container_hash) LOCAL;