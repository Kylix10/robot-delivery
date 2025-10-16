package com.example.robotdelivery;

import com.example.robotdelivery.pojo.Order;
import com.example.robotdelivery.service.ResourceManagerThread;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

/**
 * 启动类：初始化资源管理线程，模拟后端自动提交订单（无前端）
 * 对应文档“实验调试”需求
 */
@SpringBootApplication
public class RobotDeliveryApplication implements CommandLineRunner {

    @Autowired
    private ResourceManagerThread resourceManagementThread;

    public static void main(String[] args) {
        SpringApplication.run(RobotDeliveryApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. 启动资源管理线程
        resourceManagementThread.start();
        TimeUnit.SECONDS.sleep(1); // 等待线程启动

    }
}