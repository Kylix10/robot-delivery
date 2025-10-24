package com.example.robotdelivery.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MicrometerConfig {

    // 注入MeterRegistry，自动注册Spring Boot默认指标（JVM、CPU、内存等）
    @Autowired
    public void configureMetrics(MeterRegistry registry) {
        // 可在此处添加自定义指标标签（如应用版本）
        registry.config().commonTags("application", "robot-delivery-service");
    }
}