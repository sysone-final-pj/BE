package com.monito.domains.container.repository;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.container.domain.Container;
import com.monito.domains.dashboard.dto.response.ContainerDashboardResponseDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContainerRepository extends JpaRepository<Container, Long> {

    /**
     * Agent와 컨테이너 해시로 컨테이너 조회
     * @param agent Agent 엔티티
     * @param containerHash 컨테이너 해시값
     * @return Container
     */
    Optional<Container> findByAgentAndContainerHash(Agent agent, String containerHash);
}
