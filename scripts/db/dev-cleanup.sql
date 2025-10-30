-- ============================================================
-- [경고] 개발 환경 전용 DB 초기화 스크립트
-- ============================================================
-- 용도: 로컬 개발 환경에서 Flyway 재실행을 위한 DB 초기화
-- 주의: 절대 운영(PROD) 환경에서 실행하지 마세요!
-- 실행 후: 애플리케이션 재시작하여 Flyway 마이그레이션 자동 실행
-- ============================================================

-- 사용자 확인 (운영 DB 접속 방지)
-- 운영 DB 사용자 이름이 SCOTT이 아니라면 아래 조건 수정

DECLARE
    v_user VARCHAR2(30);
BEGIN
    SELECT USER INTO v_user FROM DUAL;
    IF v_user NOT IN ('SCOTT', 'DEV_USER') THEN
          RAISE_APPLICATION_ERROR(-20001,
            '경고: 이 스크립트는 개발 환경 전용입니다. 현재 사용자: ' || v_user);
    END IF;
END;
/

  -- 모든 유저 테이블 삭제
BEGIN
    FOR c IN (SELECT table_name FROM user_tables) LOOP
        BEGIN
            EXECUTE IMMEDIATE ('DROP TABLE "' || c.table_name || '" CASCADE CONSTRAINTS');
            DBMS_OUTPUT.PUT_LINE('Dropped table: ' || c.table_name);
        EXCEPTION
           WHEN OTHERS THEN
              DBMS_OUTPUT.PUT_LINE('Failed to drop: ' || c.table_name || ' - ' || SQLERRM);
        END;
    END LOOP;
END;
/

  -- 모든 시퀀스 삭제
BEGIN
    FOR s IN (SELECT sequence_name FROM user_sequences) LOOP
        EXECUTE IMMEDIATE ('DROP SEQUENCE ' || s.sequence_name);
        DBMS_OUTPUT.PUT_LINE('Dropped sequence: ' || s.sequence_name);
    END LOOP;
END;
/

-- recycle bean 지우기
PURGE RECYCLEBIN;

-- 확인 (모든 결과가 0으로 나와야 함)
SELECT 'Tables remaining: ' || COUNT(*) AS result FROM user_tables;
SELECT 'Sequences remaining: ' || COUNT(*) AS result FROM user_sequences;
SELECT COUNT(*) FROM user_recyclebin;
