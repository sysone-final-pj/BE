package com.monito.domains.alert.domain;

import com.monito.domains.container.domain.Container;
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
import org.hibernate.type.NumericBooleanConverter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
/**
 공동 작성자: 백승준, 이지민
 */
@Entity
@Table(
        name = "alerts",
        indexes = {
                @Index(name = "IDX_ALERT_IS_READ", columnList = "is_read"),
                @Index(name = "IDX_ALERT_MEMBER_IS_READ", columnList = "member_id, is_read"),
                @Index(name = "IDX_ALERT_ALERT_LEVEL", columnList = "alert_level"),
                @Index(name = "IDX_ALERT_METRIC_TYPE", columnList = "metric_type"),
                @Index(name = "IDX_ALERT_COLLECTED_AT", columnList = "collected_at"),
                @Index(name = "IDX_ALERT_CONTAINER_ID", columnList = "container_id"),
                @Index(name = "IDX_ALERT_CREATED_AT", columnList = "created_at")
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
    @Convert(converter = NumericBooleanConverter.class)
    private Boolean isRead = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 20)
    private AlertLevel alertLevel;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
