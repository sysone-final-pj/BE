package com.monito.domains.agent.domain;

import com.monito.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
        name = "agents",
        indexes = {
                @jakarta.persistence.Index(name = "IDX_AGENT_HOST_IP", columnList = "host_ip"),
                @jakarta.persistence.Index(name = "IDX_AGENT_API_TOKEN", columnList = "api_token")
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

    @Column(nullable = false, length = 100)
    private String agentName;

    @Column(nullable = false, length = 50)
    private String hostIp;

    @Column(nullable = false)
    private Integer hostPort;

    @Column(nullable = false, length = 50)
    private String osType;

    @Column(nullable = false, length = 50)
    private String dockerVersion;

    @Column(nullable = false, length = 255)
    private String apiToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AgentStatus agentStatus;

    public void updateStatus(AgentStatus status) {
        this.agentStatus = status;
    }

    public void updateAgentInfo(String agentName, String hostIp, Integer hostPort) {
        if (agentName != null) this.agentName = agentName;
        if (hostIp != null) this.hostIp = hostIp;
        if (hostPort != null) this.hostPort = hostPort;
    }
}
