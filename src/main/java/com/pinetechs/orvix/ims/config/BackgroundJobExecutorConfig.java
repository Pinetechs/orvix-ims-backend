package com.pinetechs.orvix.ims.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class BackgroundJobExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService backgroundJobExecutor() {
        return Executors.newFixedThreadPool(3);
    }
}