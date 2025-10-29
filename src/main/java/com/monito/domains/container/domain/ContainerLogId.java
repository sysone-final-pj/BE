package com.monito.domains.container.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ContainerLogId implements Serializable {
    private Long id;
    private LocalDateTime loggedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContainerLogId that = (ContainerLogId) o;
        return Objects.equals(id, that.id) && Objects.equals(loggedAt, that.loggedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, loggedAt);
    }
}