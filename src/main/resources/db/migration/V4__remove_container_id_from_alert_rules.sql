-- alert_rules 테이블에서 container_id 제거
-- 이유: 사용자는 "모든 컨테이너"에 대해 하나의 규칙만 설정하고,
--       해당 임계값을 넘는 모든 컨테이너에서 알림을 받는다.
--       container_id는 alerts 테이블에만 있으면 된다.

-- 1. FK 제약조건 제거
ALTER TABLE alert_rules DROP CONSTRAINT FK_ALERT_RULE_CONTAINER;

-- 2. container_id 컬럼 제거
ALTER TABLE alert_rules DROP COLUMN container_id;

-- 3. 같은 사용자가 같은 metric_type에 대해 중복 규칙을 만들지 못하도록 UNIQUE 제약조건 추가
ALTER TABLE alert_rules ADD CONSTRAINT UK_ALERT_RULE_MEMBER_METRIC
    UNIQUE (member_id, metric_type, is_deleted);