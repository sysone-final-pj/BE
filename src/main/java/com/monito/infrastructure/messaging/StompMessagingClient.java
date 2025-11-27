package com.monito.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
/**
 작성자: 백승준
 */
@Component
@RequiredArgsConstructor
public class StompMessagingClient {
    private final SimpMessagingTemplate template;

    public void send(String topic, Object payload){
        template.convertAndSend(topic, payload);
    }
}
