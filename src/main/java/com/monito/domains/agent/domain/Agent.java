package com.monito.domains.agent.domain;

import com.monito.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
        name = "agents",
        indexes = {
                @jakarta.persistence.Index(name = "IDX_AGENT_KEY", columnList = "agent_key", unique = true),
        }
)
@SuperBuilder
@SQLRestriction("is_deleted = 0")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Agent extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "agent_seq")
    @SequenceGenerator(
            name = "agent_seq",
            sequenceName = "AGENT_SEQ",
            allocationSize = 1
    )
    private Long id;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String agentKey;

    @Column(nullable = false, length = 100)
    private String agentName;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String osType;

    @Column(nullable = false, length = 50)
    private String dockerVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AgentStatus agentStatus;

    public void updateStatus(AgentStatus status) {
        this.agentStatus = status;
    }

    public void updateAgentInfo(String agentName) {
        if (agentName != null) this.agentName = agentName;
    }

    @PrePersist
    public void generateAgentKey() {
        if (this.agentKey == null) {
            this.agentKey = UUID.randomUUID().toString();
        }
    }
}
