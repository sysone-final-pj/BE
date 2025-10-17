package com.monito.domains.dashboard.repository;

import com.monito.domains.dashboard.domain.DashboardWidget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DashboardWidgetRepository extends JpaRepository<DashboardWidget, Long> {
}
