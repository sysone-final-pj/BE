package com.monito.domains.container.repository;

import com.monito.domains.container.domain.ContainerStatsLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContainerStatsLogRepository extends JpaRepository<ContainerStatsLog, Long> {
}
