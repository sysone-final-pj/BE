package com.monito.domains.alert.domain;

import com.monito.domains.container.domain.MetricType;
import com.monito.domains.member.domain.Member;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "alert_rules")
@Builder
@Getter
@SQLRestriction("is_deleted = 0")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AlertRule {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alert_rule_seq")
    @SequenceGenerator(
            name = "alert_rule_seq",
            sequenceName = "ALERT_RULE_SEQ",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 100)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetricType metricType;

    @Column(nullable = false, precision = 1)
    private Boolean isEnabled;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal infoThreshold;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal warningThreshold;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal highThreshold;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal criticalThreshold;

    @Column(nullable = false, precision = 10)
    private Integer cooldownSeconds;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @lombok.Builder.Default
    @Column(nullable = false)
    private Boolean isDeleted = false;

    // 규칙 활성화/비활성화
    public void enable() {
        this.isEnabled = true;
    }

    public void disable() {
        this.isEnabled = false;
    }

    public void delete() {
        this.isDeleted = true;
    }

    public void updateRuleName(String ruleName) {
        if (ruleName != null && !ruleName.isBlank()) {
            this.ruleName = ruleName;
        }
    }

    public void updateThresholds(BigDecimal infoThreshold, BigDecimal warningThreshold,
                                 BigDecimal highThreshold, BigDecimal criticalThreshold) {
        if (infoThreshold != null) this.infoThreshold = infoThreshold;
        if (warningThreshold != null) this.warningThreshold = warningThreshold;
        if (highThreshold != null) this.highThreshold = highThreshold;
        if (criticalThreshold != null) this.criticalThreshold = criticalThreshold;
    }

    public void updateCooldownSeconds(Integer cooldownSeconds) {
        if (cooldownSeconds != null && cooldownSeconds > 0) {
            this.cooldownSeconds = cooldownSeconds;
        }
    }

    // 임계값에 따른 알림 레벨 판단
    public AlertLevel determineAlertLevel(BigDecimal currentValue) {
        if (criticalThreshold != null && currentValue.compareTo(criticalThreshold) >= 0) {
            return AlertLevel.CRITICAL;
        }
        if (highThreshold != null && currentValue.compareTo(highThreshold) >= 0) {
            return AlertLevel.HIGH;
        }
        if (warningThreshold != null && currentValue.compareTo(warningThreshold) >= 0) {
            return AlertLevel.WARNING;
        }
        if (infoThreshold != null && currentValue.compareTo(infoThreshold) >= 0) {
            return AlertLevel.INFO;
        }
        return null;  // 임계값 미만
    }
}
