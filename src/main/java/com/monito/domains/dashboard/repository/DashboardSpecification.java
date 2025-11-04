package com.monito.domains.dashboard.repository;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerStatsLog;
import com.monito.domains.dashboard.dto.request.ContainerFilterDTO;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 대시보드 컨테이너 필터링을 위한 Specification
 */
public class DashboardSpecification {

    /**
     * 필터 조건에 따라 동적으로 Specification 생성
     *
     * @param filter   필터 조건
     * @param memberId 회원 ID (즐겨찾기 필터링 시 사용)
     * @return Specification
     */
    public static Specification<Container> withFilter(ContainerFilterDTO filter, Long memberId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 필터가 없으면 모든 컨테이너 반환
            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            // 1. 즐겨찾기 필터
            if (filter.getFavoriteOnly() != null && filter.getFavoriteOnly() && memberId != null) {
                // Favorite 테이블과 조인하여 즐겨찾기된 컨테이너만 조회
                var subquery = query.subquery(Long.class);
                var subRoot = subquery.from(com.monito.domains.favorite.domain.Favorite.class);
                subquery.select(subRoot.get("container").get("id"))
                        .where(criteriaBuilder.equal(subRoot.get("member").get("id"), memberId));

                predicates.add(root.get("id").in(subquery));
            }

            // 2. State 필터 (다중 선택) - ContainerStatsLog 서브쿼리 사용
            if (filter.getStates() != null && !filter.getStates().isEmpty()) {
                Subquery<Long> stateSubquery = query.subquery(Long.class);
                Root<ContainerStatsLog> stateRoot = stateSubquery.from(ContainerStatsLog.class);

                // correlate 사용으로 서브쿼리 안정화
                Subquery<LocalDateTime> maxCreatedAtSubquery = stateSubquery.subquery(LocalDateTime.class);
                Root<ContainerStatsLog> correlatedRoot = maxCreatedAtSubquery.correlate(stateRoot);
                maxCreatedAtSubquery.select(
                        criteriaBuilder.greatest(correlatedRoot.<LocalDateTime>get("createdAt"))
                );

                stateSubquery.select(stateRoot.get("container").get("id"))
                        .where(
                                criteriaBuilder.and(
                                        criteriaBuilder.equal(stateRoot.get("createdAt"), maxCreatedAtSubquery),
                                        stateRoot.get("state").in(filter.getStates())
                                )
                        );

                predicates.add(root.get("id").in(stateSubquery));
            }

            // 3. Health 필터 (다중 선택) - ContainerStatsLog 서브쿼리 사용
            if (filter.getHealths() != null && !filter.getHealths().isEmpty()) {
                // 최신 ContainerStatsLog에서 health 필터 조건을 만족하는 Container ID 찾기
                Subquery<Long> healthSubquery = query.subquery(Long.class);
                Root<ContainerStatsLog> healthRoot = healthSubquery.from(ContainerStatsLog.class);

                // 각 Container의 최신 createdAt 찾기 (correlate 사용)
                Subquery<LocalDateTime> maxCreatedAtSubquery = healthSubquery.subquery(LocalDateTime.class);
                Root<ContainerStatsLog> correlatedHealthRoot = maxCreatedAtSubquery.correlate(healthRoot);

                maxCreatedAtSubquery.select(
                        criteriaBuilder.greatest(correlatedHealthRoot.<LocalDateTime>get("createdAt"))
                );

                // 최신 로그 + health 일치하는 컨테이너 ID만 선택
                healthSubquery.select(healthRoot.get("container").get("id"))
                        .where(
                                criteriaBuilder.and(
                                        criteriaBuilder.equal(healthRoot.get("createdAt"), maxCreatedAtSubquery),
                                        healthRoot.get("health").in(filter.getHealths())
                                )
                        );

                predicates.add(root.get("id").in(healthSubquery));
            }

            // 4. Agent 필터 (다중 선택)
            if (filter.getAgentIds() != null && !filter.getAgentIds().isEmpty()) {
                Join<Container, Agent> agentJoin = root.join("agent");
                predicates.add(agentJoin.get("id").in(filter.getAgentIds()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}