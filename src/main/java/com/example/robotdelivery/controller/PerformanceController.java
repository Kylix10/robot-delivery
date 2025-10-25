package com.example.robotdelivery.controller;

import com.example.robotdelivery.pojo.dto.PerformanceComparisonDTO;
import com.example.robotdelivery.service.IPerformanceService;
import com.example.robotdelivery.service.IPerformanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/performance")
public class PerformanceController {
    @Autowired
    private IPerformanceService performanceService;

    // 前端调用此接口获取最新性能对比数据
    @GetMapping("/comparison")
    public PerformanceComparisonDTO getLatestPerformanceComparison() {
        return performanceService.getLatestPerformanceComparison();
    }
}