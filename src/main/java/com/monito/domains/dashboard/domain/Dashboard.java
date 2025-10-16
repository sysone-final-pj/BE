package com.monito.domains.dashboard.domain;

import com.monito.domains.member.domain.Member;
import com.monito.global.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "dashboards")
@SuperBuilder
@SQLRestriction("is_deleted = 0")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Dashboard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dashboard_seq")
    @SequenceGenerator(
            name = "dashboard_seq",
            sequenceName = "DASHBOARD_SEQ",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private Boolean isDefault;

    @OneToMany(mappedBy = "dashboard", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DashboardWidget> widgets = new ArrayList<>();

    public void updateName(String name) {
        this.name = name;
    }

    public void setAsDefault() {
        this.isDefault = true;
    }

    public void unsetDefault() {
        this.isDefault = false;
    }

    public void addWidget(DashboardWidget widget) {
        this.widgets.add(widget);
        widget.setDashboard(this);
    }

    public void removeWidget(DashboardWidget widget) {
        this.widgets.remove(widget);
        widget.setDashboard(null);
    }
}