package com.example.robotdelivery.pojo.dto;
import lombok.Data;

// 性能对比DTO：包含两种模式的核心指标
@Data
public class PerformanceComparisonDTO {
    // 算法模式（mode_type=1）
    private ModePerformanceDTO algorithmMode;
    // 默认模式（mode_type=2）
    private ModePerformanceDTO defaultMode;
    // 状态：success/error（用于前端判断是否有数据）
    private String status;
    // 提示信息（如无数据时的说明）
    private String message;

    // 内部类：单种模式的性能指标
    @Data
    public static class ModePerformanceDTO {
        private Integer completedCount; // 完成订单数
        private Double totalRevenue;    // 总收益
        private Double avgResponseTimeMs; // 平均响应时间（毫秒）
        private Double throughput;      // 吞吐量（订单/秒）
        private String calcTime;        // 计算时间（前端展示用）
    }
}