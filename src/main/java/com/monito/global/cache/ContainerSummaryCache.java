package com.monito.global.cache;

import com.monito.domains.container.dto.response.ContainerSummaryResponseDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ContainerSummaryCache {

    private final ConcurrentHashMap<Long, ContainerSummaryResponseDTO> cache = new ConcurrentHashMap<>();

    public void update(ContainerSummaryResponseDTO summary) {
        cache.put(summary.getId(), summary);
    }

    public List<ContainerSummaryResponseDTO> snapshot() {
        return List.copyOf(cache.values());
    }

    public void remove(Long containerId) {
        cache.remove(containerId);
    }
}