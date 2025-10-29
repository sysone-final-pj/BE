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
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
        name = "container_logs",
        indexes = {
                @jakarta.persistence.Index(name = "IDX_CONTAINER_LOG_LOGGED_AT", columnList = "logged_at"),
                @jakarta.persistence.Index(name = "IDX_CONTAINER_LOG_CONTAINER_LOGGED_AT", columnList = "container_id, logged_at")
        }
)
@IdClass(ContainerLogId.class)
@SuperBuilder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ContainerLog {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "container_log_seq")
    @SequenceGenerator(
            name = "container_log_seq",
            sequenceName = "CONTAINER_LOG_SEQ",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id", nullable = false)
    private Container container;

    @Lob
    @Column(nullable = false)
    private String logMessage;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LogSource source;

    @Id
    @Column(nullable = false)
    private LocalDateTime loggedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
