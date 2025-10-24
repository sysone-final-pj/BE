package com.monito.domains.container.domain;

import com.monito.domains.agent.domain.Agent;
import com.monito.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
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

    /**
     리소스 제한 설정 (default 100,000 µs) -> 0.1초 단위로 CPU 할당 계산
    */
    @Column(nullable = false)
    private Long cpuQuota;

    // 주기 내 사용 가능한 CPU 시간 (µs 단위)
    @Column(nullable = false)
    private Long cpuPeriod;

    @Column(nullable = false)
    private Long cpuLimit;

    @Column(nullable = false)
    private Integer onlineCpus;

    @Column(nullable = false)
    private Long memLimit;

    @Column(nullable = false)
    private Integer oomKills;

    @PrePersist
    private void prePersist() {
        if (this.oomKills == null) {
            this.oomKills = 0;
        }
    }
}
