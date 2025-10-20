package com.monito.domains.alert.domain;

import com.monito.domains.container.domain.MetricType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "alert_rules")
@Builder
@Getter
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

    @Column(nullable = false, length = 100)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetricType metricType;

    @Column(nullable = false, precision = 1)
    private Boolean isEnabled;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal warningThreshold;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal criticalThreshold;

    @Column(nullable = false, precision = 10)
    private Integer durationSeconds;

    @Column(nullable = false, precision = 10)
    private Integer cooldownSeconds;

    @Column(nullable = false, precision = 10)
    private Integer checkInterval;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
