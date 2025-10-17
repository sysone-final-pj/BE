package com.monito.domains.dashboard.repository;

import com.monito.domains.dashboard.domain.WidgetTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WidgetTemplateRepository extends JpaRepository<WidgetTemplate, Long> {
}
