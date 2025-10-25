package com.example.robotdelivery.pojo.dto;

import lombok.Data;
import java.util.List;

@Data
public class PerformanceComparisonDTO {
    // 算法模式（mode_type=1）
    private ModePerformanceDTO algorithmMode;
    // 默认模式（mode_type=2）
    private ModePerformanceDTO defaultMode;
    private List<ModePerformanceDTO> algorithmHistory; // 历史-算法模式
    private List<ModePerformanceDTO> defaultHistory;   // 历史-默认模式
    // 状态：success/error（用于前端判断是否有数据）
    private String status;
    // 提示信息（如无数据时的说明）
    private String message;

    // 内部类：单种模式的性能指标（已补充setter，参考之前的修复）
    @Data
    public static class ModePerformanceDTO {
        private Integer completedCount;
        private Double totalRevenue;
        private Double avgResponseTimeMs;
        private Double throughput;
        private String calcTime;

        // 手动生成ModePerformanceDTO的setter和getter（之前已补充）
        public void setCompletedCount(Integer completedCount) {
            this.completedCount = completedCount;
        }
        public void setTotalRevenue(Double totalRevenue) {
            this.totalRevenue = totalRevenue;
        }
        public void setAvgResponseTimeMs(Double avgResponseTimeMs) {
            this.avgResponseTimeMs = avgResponseTimeMs;
        }
        public void setThroughput(Double throughput) {
            this.throughput = throughput;
        }
        public void setCalcTime(String calcTime) {
            this.calcTime = calcTime;
        }
        public Integer getCompletedCount() { return completedCount; }
        public Double getTotalRevenue() { return totalRevenue; }
        public Double getAvgResponseTimeMs() { return avgResponseTimeMs; }
        public Double getThroughput() { return throughput; }
        public String getCalcTime() { return calcTime; }
    }

    // 手动生成外部类PerformanceComparisonDTO的setter方法（解决当前报错）
    public void setAlgorithmMode(ModePerformanceDTO algorithmMode) {
        this.algorithmMode = algorithmMode;
    }
    public void setDefaultMode(ModePerformanceDTO defaultMode) {
        this.defaultMode = defaultMode;
    }
    public void setAlgorithmHistory(List<ModePerformanceDTO> algorithmHistory) {
        this.algorithmHistory = algorithmHistory;
    }
    public void setDefaultHistory(List<ModePerformanceDTO> defaultHistory) {
        this.defaultHistory = defaultHistory;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    // 如需使用getter，也需手动生成（根据需要添加）
    public ModePerformanceDTO getAlgorithmMode() { return algorithmMode; }
    public ModePerformanceDTO getDefaultMode() { return defaultMode; }
    public List<ModePerformanceDTO> getAlgorithmHistory() { return algorithmHistory; }
    public List<ModePerformanceDTO> getDefaultHistory() { return defaultHistory; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
}