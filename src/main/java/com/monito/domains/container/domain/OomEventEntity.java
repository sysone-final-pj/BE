package com.monito.domains.container.domain;

import com.monito.global.cache.OomEvent;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "oom_events")
@SuperBuilder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class OomEventEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "oom_event_seq")
    @SequenceGenerator(
            name = "oom_event_seq",
            sequenceName = "OOM_EVENT_SEQ",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id", nullable = false)
    private Container container;

    @Column(nullable = false, length = 64)
    private String containerHash;

    @Column(nullable = false, length = 50)
    private String containerName;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Column(length = 255)
    private String agentKey;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static OomEventEntity from(OomEvent cacheEvent, Container container) {
        return OomEventEntity.builder()
                .container(container)
                .containerHash(cacheEvent.getContainerHash())
                .containerName(cacheEvent.getContainerName())
                .occurredAt(cacheEvent.getOccurredAt())
                .agentKey(cacheEvent.getAgentKey())
                .build();
    }

    public OomEvent toCacheEvent() {
        return OomEvent.builder()
                .containerId(container.getId())
                .containerHash(containerHash)
                .containerName(containerName)
                .occurredAt(occurredAt)
                .agentKey(agentKey)
                .build();
    }
}