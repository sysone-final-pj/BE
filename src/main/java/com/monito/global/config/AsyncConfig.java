package com.monito.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 처리 설정
 * - WebSocket 메시지 처리 (LOGS, METRICS)를 비동기로 처리하여 응답 지연 방지
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * WebSocket 메시지 처리용 스레드 풀
     * - LOGS, METRICS 처리를 비동기로 수행
     */
    @Bean(name = "websocketTaskExecutor")
    public Executor websocketTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 코어 스레드 수: Agent 7개 × 1.5 = 10개 (동시 처리)
        executor.setCorePoolSize(10);

        // 최대 스레드 수: 40개 (피크 시간, 버스트 대응)
        executor.setMaxPoolSize(40);

        // 큐 용량: 300개 (대기 가능한 작업 수, 70 컨테이너 × 4 = 280)
        executor.setQueueCapacity(300);

        // 스레드 이름 접두사
        executor.setThreadNamePrefix("ws-async-");

        // 큐가 가득 찰 때 정책: 호출자 스레드에서 실행 (요청 유실 방지)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 스레드 풀 종료 대기 시간
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("WebSocket 비동기 처리 스레드 풀 초기화 완료 - core: {}, max: {}, queue: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return websocketTaskExecutor();
    }
}
