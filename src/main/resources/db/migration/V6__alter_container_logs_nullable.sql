-- log_message 컬럼을 nullable로 변경
-- LOB 컬럼은 직접 MODIFY 불가하므로 임시 컬럼을 이용한 재생성

-- 1. 임시 컬럼 추가 (nullable)
ALTER TABLE container_logs ADD log_message_temp CLOB;

-- 2. 데이터 복사
UPDATE container_logs SET log_message_temp = log_message;

-- 3. 기존 컬럼 삭제 (NOT NULL 제약 조건도 함께 삭제됨)
ALTER TABLE container_logs DROP COLUMN log_message;

-- 4. 임시 컬럼 이름 변경
ALTER TABLE container_logs RENAME COLUMN log_message_temp TO log_message;
