package com.monito.domains.container.repository;

import com.monito.domains.container.domain.ContainerLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContainerLogRepository extends JpaRepository<ContainerLog, Long> {
}
