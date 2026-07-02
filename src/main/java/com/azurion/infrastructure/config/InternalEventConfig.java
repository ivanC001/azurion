package com.azurion.infrastructure.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@Configuration
public class InternalEventConfig {

    @Bean(name = "applicationEventMulticaster")
    public SimpleApplicationEventMulticaster applicationEventMulticaster() {
        SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
        multicaster.setTaskExecutor(eventExecutor());
        return multicaster;
    }

    @Bean
    public Executor eventExecutor() {
        return new SimpleAsyncTaskExecutor("azurion-event-");
    }
}
