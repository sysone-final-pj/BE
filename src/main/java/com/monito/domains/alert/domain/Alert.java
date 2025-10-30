package com.monito.domains.alert.domain;

import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.MetricType;
import com.monito.domains.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
        name = "alerts",
        indexes = {
                @jakarta.persistence.Index(name = "IDX_ALERT_IS_READ", columnList = "is_read"),
                @jakarta.persistence.Index(name = "IDX_ALERT_MEMBER_IS_READ", columnList = "member_id, is_read"),
                @jakarta.persistence.Index(name = "IDX_ALERT_ALERT_LEVEL", columnList = "alert_level"),
                @jakarta.persistence.Index(name = "IDX_ALERT_METRIC_TYPE", columnList = "metric_type"),
                @jakarta.persistence.Index(name = "IDX_ALERT_COLLECTED_AT", columnList = "collected_at"),
                @jakarta.persistence.Index(name = "IDX_ALERT_CONTAINER_ID", columnList = "container_id"),
                @jakarta.persistence.Index(name = "IDX_ALERT_CREATED_AT", columnList = "created_at")
        }
)
@Builder
@SQLRestriction("is_deleted = 0")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "alert_seq")
    @SequenceGenerator(
            name = "alert_seq",
            sequenceName = "ALERT_SEQ",
            allocationSize = 1
    )
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private AlertRule alertRule;

    // TODO : N+1 문제 고려해보기
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id")
    private Container container;

    @Column(nullable = false, length = 255)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 255)
    private MetricType metricType;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal metricValue;

    @lombok.Builder.Default
    @Column(nullable = false)
    private Boolean isRead = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 20)
    private AlertLevel alertLevel;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @lombok.Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    public void markAsRead() {
        this.isRead = true;
    }

    public void delete() {
        this.isDeleted = true;
    }
}
