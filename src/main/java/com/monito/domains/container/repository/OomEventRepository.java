package com.monito.domains.container.repository;

import com.monito.domains.container.domain.OomEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OomEventRepository extends JpaRepository<OomEventEntity, Long> {

    /**
     * 특정 시간 이후의 모든 OOM 이벤트 조회 (서버 시작 시 캐시 초기화용)
     */
    @Query("SELECT o FROM OomEventEntity o WHERE o.occurredAt >= :startTime ORDER BY o.occurredAt ASC")
    List<OomEventEntity> findAllAfter(@Param("startTime") LocalDateTime startTime);

    /**
     * 특정 컨테이너의 시간 범위 내 OOM 이벤트 조회
     */
    @Query("SELECT o FROM OomEventEntity o WHERE o.container.id = :containerId " +
           "AND o.occurredAt >= :startTime AND o.occurredAt <= :endTime " +
           "ORDER BY o.occurredAt ASC")
    List<OomEventEntity> findByContainerIdAndTimeRange(
            @Param("containerId") Long containerId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 특정 시간 이전의 OOM 이벤트 삭제 (TTL 관리용)
     */
    void deleteByOccurredAtBefore(LocalDateTime cutoffTime);
}