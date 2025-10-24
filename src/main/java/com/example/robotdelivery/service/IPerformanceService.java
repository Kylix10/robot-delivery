package com.example.robotdelivery.service;

import com.example.robotdelivery.pojo.dto.PerformanceComparisonDTO;

// 接口类规范：用interface定义，包含抽象方法
public interface IPerformanceService {
    // 统一方法：获取两种模式的最新性能对比数据
    PerformanceComparisonDTO getLatestPerformanceComparison();
}