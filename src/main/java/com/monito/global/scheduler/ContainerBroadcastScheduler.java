package com.monito.global.scheduler;

import com.monito.global.cache.ContainerSummaryCache;
import com.monito.infrastructure.messaging.StompMessagingClient;
import com.monito.infrastructure.messaging.WsTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContainerBroadcastScheduler {
    private final StompMessagingClient messagingClient;
    private final ContainerSummaryCache containerSummaryCache;

    @Scheduled(
            fixedDelayString = "${app.scheduler.container-summary-push.fixedDelay}",
            initialDelayString = "${app.scheduler.container-summary-push.initialDelay}"
    )
    public void broadcastSummary() {
        messagingClient.send(WsTopics.CONTAINER_SUMMARY, containerSummaryCache.snapshot());
    }
}
