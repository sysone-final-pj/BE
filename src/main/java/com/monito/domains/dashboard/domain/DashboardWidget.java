package com.monito.domains.dashboard.domain;

import com.monito.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "dashboard_widget",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_DASHBOARD_POSITION",
                        columnNames = {"dashboard_id", "grid_position"}
                )
        }
)
@SuperBuilder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardWidget extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dashboard_widget_seq")
    @SequenceGenerator(
            name = "dashboard_widget_seq",
            sequenceName = "DASHBOARD_WIDGET_SEQ",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dashboard_id", nullable = false)
    private Dashboard dashboard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "widget_template_id", nullable = false)
    private WidgetTemplate widgetTemplate;

    @Column(nullable = false)
    private Integer gridPosition;

    public void updatePosition(Integer gridPosition) {
        this.gridPosition = gridPosition;
    }

    protected void setDashboard(Dashboard dashboard) {
        this.dashboard = dashboard;
    }
}