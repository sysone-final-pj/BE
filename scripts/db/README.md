# Database Scripts

## 개발 환경 전용 스크립트

### dev-cleanup.sql
로컬 개발 환경에서 DB를 초기화하는 스크립트입니다.

**⚠️ 경고: 절대 운영 환경에서 실행하지 마세요!**

**사용 시점:**
- Flyway 마이그레이션을 처음부터 다시 실행하고 싶을 때
- DDL 파일 수정 후 테스트할 때
- 개발 DB가 꼬였을 때

**실행 방법:**
  ```bash
  # IntelliJ Database Console에서
  @scripts/db/dev-cleanup.sql

  # 또는 SQL*Plus에서
  sqlplus SCOTT/password@localhost:1521/XEPDB1
  @scripts/db/dev-cleanup.sql
```
  실행 후:
  1. 애플리케이션 재시작
  2. Flyway가 자동으로 V1, V2, V3 마이그레이션 실행
  3. 깨끗한 스키마 재구성 완료
