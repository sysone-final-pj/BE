package com.monito.domains.alert.repository;

import com.monito.domains.alert.domain.Alert;
import com.monito.domains.alert.dto.request.AlertFilterDTO;
import com.monito.domains.alert.dto.request.AlertSortType;
import com.monito.domains.agent.domain.Agent;
import com.monito.domains.container.domain.Container;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class AlertSpecification {

    /**
     * 필터 조건에 따라 동적으로 Specification 생성
     */
    public static Specification<Alert> withFilter(Long memberId, AlertFilterDTO filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 필수 조건: memberId 및 isDeleted = false
            predicates.add(criteriaBuilder.equal(root.get("member").get("id"), memberId));
            predicates.add(criteriaBuilder.equal(root.get("isDeleted"), false));

            // Container 조인 (containerName 필터나 CONTAINER_NAME 정렬에 필요)
            Join<Alert, Container> containerJoin = null;
            boolean needsContainerJoin = false;

            // 선택적 필터 조건
            if (filter != null) {
                // alertLevel
                if (filter.getAlertLevel() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("alertLevel"), filter.getAlertLevel()));
                }

                // metricType
                if (filter.getMetricType() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("metricType"), filter.getMetricType()));
                }

                // isRead
                if (filter.getIsRead() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("isRead"), filter.getIsRead()));
                }

                // collectedAt 범위
                if (filter.getCollectedAtFrom() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                            root.get("collectedAt"), filter.getCollectedAtFrom()));
                }
                if (filter.getCollectedAtTo() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(
                            root.get("collectedAt"), filter.getCollectedAtTo()));
                }

                // createdAt 범위
                if (filter.getCreatedAtFrom() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                            root.get("createdAt"), filter.getCreatedAtFrom()));
                }
                if (filter.getCreatedAtTo() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(
                            root.get("createdAt"), filter.getCreatedAtTo()));
                }

                // containerName 필터 체크
                if (filter.getContainerName() != null && !filter.getContainerName().isBlank()) {
                    needsContainerJoin = true;
                }

                // sortType이 CONTAINER_NAME인지 체크
                if (filter.getSortType() == AlertSortType.CONTAINER_NAME) {
                    needsContainerJoin = true;
                }

                // Container 조인이 필요하면 생성
                if (needsContainerJoin) {
                    containerJoin = root.join("container");
                }

                // containerName - Container 조인 필요
                if (filter.getContainerName() != null && !filter.getContainerName().isBlank()) {
                    predicates.add(criteriaBuilder.like(
                            containerJoin.get("name"),
                            "%" + filter.getContainerName() + "%"));
                }

                // agentName - Container -> Agent 조인 필요
                if (filter.getAgentName() != null && !filter.getAgentName().isBlank()) {
                    if (containerJoin == null) {
                        containerJoin = root.join("container");
                    }
                    Join<Container, Agent> agentJoin = containerJoin.join("agent");
                    predicates.add(criteriaBuilder.like(
                            agentJoin.get("agentName"),
                            "%" + filter.getAgentName() + "%"));
                }

                // 동적 정렬
                Order order = getOrderBy(filter.getSortType(), root, containerJoin, criteriaBuilder);
                query.orderBy(order);
            } else {
                // 기본 정렬 (최신순)
                query.orderBy(criteriaBuilder.desc(root.get("createdAt")));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 정렬 타입에 따라 Order 생성
     */
    private static Order getOrderBy(
            AlertSortType sortType,
            jakarta.persistence.criteria.Root<Alert> root,
            Join<Alert, Container> containerJoin,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder) {

        if (sortType == null) {
            // 기본 정렬: 생성 시간 내림차순 (최신순)
            return criteriaBuilder.desc(root.get("createdAt"));
        }

        return switch (sortType) {
            case ALERT_LEVEL -> criteriaBuilder.desc(root.get("alertLevel"));
            case METRIC_TYPE -> criteriaBuilder.asc(root.get("metricType"));
            case CONTAINER_NAME -> criteriaBuilder.asc(containerJoin.get("name"));
            case METRIC_VALUE -> criteriaBuilder.desc(root.get("metricValue"));
            case COLLECTED_AT -> criteriaBuilder.desc(root.get("collectedAt"));
        };
    }
}