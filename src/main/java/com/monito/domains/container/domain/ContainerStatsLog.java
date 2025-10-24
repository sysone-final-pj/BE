package com.monito.domains.container.domain;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
        name = "container_stats_logs",
        indexes = {
                @jakarta.persistence.Index(name = "IDX_CONTAINER_STATS_CREATED_AT", columnList = "created_at"),
                @jakarta.persistence.Index(name = "IDX_CONTAINER_STATS_CONTAINER_CREATED_AT", columnList = "container_id, created_at")
        }
)
@SuperBuilder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ContainerStatsLog {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "container_stats_log_seq")
    @SequenceGenerator(
            name = "container_stats_log_seq",
            sequenceName = "CONTAINER_STATS_LOG_SEQ",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id", nullable = false)
    private Container container;

    @Column(nullable = false, length = 64)
    private String containerHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ContainerState state;

    // CPU 관련
    @Column(precision = 6, scale = 2)
    private BigDecimal cpuPercent;

    @Column(nullable = false)
    private Long hostCpuUsageTotal;

    @Column(nullable = false)
    private Long cpuUsageTotal;

    @Column(nullable = false)
    private Long cpuUser;

    @Column(nullable = false)
    private Long cpuSystem;

    @Column(nullable = false)
    private Long cpuQuota;

    @Column(nullable = false)
    private Long cpuPeriod;

    @Column(nullable = false)
    private Long cpuLimit;

    @Column(nullable = false)
    private Integer onlineCpus;

    @Column(nullable = false)
    private Long throttlingPeriods;

    @Column(nullable = false)
    private Long throttledPeriods;

    @Column(nullable = false)
    private Long throttledTime;

    // Memory 관련
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal memPercent;

    @Column(nullable = false)
    private Long memUsage;

    @Column(nullable = false)
    private Long memLimit;

    @Column(nullable = false)
    private Long memMaxUsage;

    // Block I/O 관련
    @Column(nullable = false)
    private Long blkRead;

    @Column(nullable = false)
    private Long blkWrite;

    // Network 관련
    @Column(nullable = false)
    private Long rxBytes;

    @Column(nullable = false)
    private Long txBytes;

    @Column(nullable = false)
    private Long rxMbps;

    @Column(nullable = false)
    private Long txMbps;

    @Column(nullable = false)
    private Long rxPps;

    @Column(nullable = false)
    private Long txPps;

    @Column(nullable = false)
    private Integer rxErrors;

    @Column(nullable = false)
    private Integer txErrors;

    @Column(nullable = false)
    private Integer rxDropped;

    @Column(nullable = false)
    private Integer txDropped;

    // 메트릭 수집 시간 (Agent에서 실제로 수집한 시간)
    @Column(nullable = false)
    private LocalDateTime collectedAt;

    // DB 저장 시간 (Backend에서 INSERT한 시간)
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
