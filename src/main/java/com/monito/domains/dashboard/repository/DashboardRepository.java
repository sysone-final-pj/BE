package com.monito.domains.dashboard.repository;

import com.monito.domains.dashboard.domain.Dashboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DashboardRepository extends JpaRepository<Dashboard, Long> {
}
