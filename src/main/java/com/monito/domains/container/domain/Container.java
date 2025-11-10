package com.monito.domains.container.domain;

import com.monito.domains.agent.domain.Agent;
import com.monito.global.common.entity.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

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
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(nullable = false, length = 64)
    private String containerHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContainerState state;

    @Column(length = 100)
    private String status;

    @Column(nullable = false, length = 50)
    private String name;

    /**
     * CPU 할당 시간 제한 (µs 단위)
     * 예: 150000 = 150ms (1.5 코어 분량)
     */
    @Column(nullable = false)
    private Long cpuQuota;

    /**
     * CPU 스케줄링 주기 (µs 단위, default 100000 = 100ms)
     */
    @Column(nullable = false)
    private Long cpuPeriod;

    /**
     * CPU 제한 (코어 단위)
     * cpuQuota / cpuPeriod로 계산
     * 예: 1.5 (1.5 코어)
     */
    @Column(precision = 6, scale = 2)
    private BigDecimal cpuLimitCores;

    /**
     * 컨테이너가 스케줄링될 수 있는 CPU 코어 수
     * (일반적으로 호스트 전체 코어, cpuset 제한 시 다를 수 있음)
     */
    @Column(nullable = false)
    private Integer onlineCpus;

    /**
     * CPU 제한이 없는지 여부 (unlimited)
     * - true: 호스트 전체 CPU 사용 가능
     * - false: cpuQuota/cpuPeriod로 제한됨
     */
    @Column(nullable = false)
    private Boolean isCpuUnlimited;

    @Column(nullable = false)
    private Long memLimit;

    /**
     * 메모리 제한이 없는지 여부 (unlimited)
     * - true: 호스트 전체 메모리 사용 가능
     * - false: memLimit으로 제한됨
     */
    @Column(nullable = false)
    private Boolean isMemoryUnlimited;

    @Column(nullable = false)
    private Integer oomKills;

    /**
     * 마지막 OOM Kill 발생 시각
     * - 중복 이벤트 방지를 위해 사용
     * - null: 한 번도 OOM이 발생하지 않음
     */
    @Column
    private LocalDateTime lastOomKilledAt;

    /**
     * 스토리지 할당량 (bytes)
     * - Docker의 --storage-opt size 옵션으로 설정 가능
     * - 0인 경우 무제한 (Agent 호스트의 전체 디스크 용량 사용)
     */
    @Column(nullable = false)
    private Long storageLimit;

    /**
     * 스토리지 제한이 없는지 여부 (unlimited)
     * - true: 호스트 전체 디스크 용량 사용 가능
     * - false: storageLimit으로 제한됨
     */
    @Column(nullable = false)
    private Boolean isStorageUnlimited;


    /**
     * 컨테이너 이미지 이름
     * 예: "nginx:latest", "ubuntu:20.04"
     */
    @Column(length = 255)
    private String imageName;

    /**
     * 컨테이너 이미지 크기 (bytes)
     * 컨테이너 생성 시점에 결정되며 변경되지 않음
     */
    @Column
    private Long imageSize;

    @PrePersist
    private void prePersist() {
        if (this.oomKills == null) {
            this.oomKills = 0;
        }
    }

    public void updateSpecs(Long cpuQuota,
                            Long cpuPeriod,
                            BigDecimal cpuLimitCores,
                            Integer onlineCpus,
                            Boolean isCpuUnlimited,
                            Long memLimit,
                            Boolean isMemoryUnlimited,
                            Long storageLimit,
                            Boolean isStorageUnlimited) {

        this.cpuQuota = cpuQuota;
        this.cpuPeriod = cpuPeriod;
        this.cpuLimitCores = cpuLimitCores;
        this.onlineCpus = onlineCpus;
        this.isCpuUnlimited = isCpuUnlimited;
        this.memLimit = memLimit;
        this.isMemoryUnlimited = isMemoryUnlimited;
        this.storageLimit = storageLimit;
        this.isStorageUnlimited = isStorageUnlimited;
    }

    public void changeState(ContainerState state){
        this.state = state;
    }

    public void changeStateWithStatus(ContainerState state, String status) {
        this.state = state;
        this.status = status;
    }

    public void incrementOomKills() {
        this.oomKills++;
    }

    /**
     * 마지막 OOM Kill 발생 시각 업데이트
     * @param occurredAt OOM 발생 시각
     */
    public void updateLastOomKilledAt(LocalDateTime occurredAt) {
        this.lastOomKilledAt = occurredAt;
    }
}
