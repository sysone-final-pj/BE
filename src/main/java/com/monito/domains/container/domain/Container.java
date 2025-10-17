package com.monito.domains.container.domain;

import com.monito.domains.agent.domain.Agent;
import com.monito.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "containers")
@SuperBuilder
@SQLRestriction("is_deleted = 0")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Container extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "container_seq")
    @SequenceGenerator(
            name = "container_seq",
            sequenceName = "CONTAINER_SEQ",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(nullable = false, length = 64)
    private String containerHash;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ContainerStatus status;

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

    @Column(nullable = false)
    private Integer oomKills;

    // Memory 관련
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal memPercent;

    @Column(nullable = false)
    private Long memUsage;

    @Column(nullable = false)
    private Long memLimit;

    @Column(nullable = false)
    private Long memMaxUsage;

    @Column(nullable = false)
    private Long memRss;

    @Column(nullable = false)
    private Long memCache;

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

    public void updateStats(ContainerStatus status, BigDecimal cpuPercent, BigDecimal memPercent,
                            Long cpuUsageTotal, Long memUsage, Long rxBytes, Long txBytes) {
        this.status = status;
        this.cpuPercent = cpuPercent;
        this.memPercent = memPercent;
        this.cpuUsageTotal = cpuUsageTotal;
        this.memUsage = memUsage;
        this.rxBytes = rxBytes;
        this.txBytes = txBytes;
    }
}
