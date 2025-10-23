package com.example.robotdelivery.pojo.vo;

public class PerformanceVo {
    // 基础算法指标
    private double noAlgAvgResponseTime; // 不使用算法的平均响应时间 (分钟)
    private double noAlgRevenue;         // 不使用算法的当前总收益/每小时平均收益 (元)

    // 优化算法指标
    private double algAvgResponseTime;   // 使用算法的平均响应时间 (分钟)
    private double algRevenue;           // 使用算法的当前总收益/每小时平均收益 (元)


    public double getNoAlgAvgResponseTime() {
        return noAlgAvgResponseTime;
    }

    public void setNoAlgAvgResponseTime(double noAlgAvgResponseTime) {
        this.noAlgAvgResponseTime = noAlgAvgResponseTime;
    }

    public double getNoAlgRevenue() {
        return noAlgRevenue;
    }

    public void setNoAlgRevenue(double noAlgRevenue) {
        this.noAlgRevenue = noAlgRevenue;
    }

    public double getAlgAvgResponseTime() {
        return algAvgResponseTime;
    }

    public void setAlgAvgResponseTime(double algAvgResponseTime) {
        this.algAvgResponseTime = algAvgResponseTime;
    }

    public double getAlgRevenue() {
        return algRevenue;
    }

    public void setAlgRevenue(double algRevenue) {
        this.algRevenue = algRevenue;
    }
}
