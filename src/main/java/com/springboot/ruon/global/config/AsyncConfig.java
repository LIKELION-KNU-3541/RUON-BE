package com.springboot.ruon.global.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


 //Vision 호출당 과금 발생 이슈 방지를 위해 동시 실행 수에 상한을 둔다.
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String SCAN_EXECUTOR = "scanExecutor";

    @Bean(SCAN_EXECUTOR)
    public Executor scanExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("scan-");
        executor.initialize();
        return executor;
    }
}
