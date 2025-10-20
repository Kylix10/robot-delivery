package com.example.robotdelivery.vo;

import lombok.Data;
import java.util.Map;

@Data
public class OrderScheduleResult {
    private String dishName; // 订单菜品名
    private Map<String, SchedulerResult> algorithmResults; // key=算法名(FCFS/SSTF/SCAN), value=调度结果
    public void setDishName(String dishName) {
        this.dishName = dishName;
    }
    public void setAlgorithmResults(Map<String, SchedulerResult> algorithmResults) {
        this.algorithmResults = algorithmResults;
    }

    public String getDishName() {
        return dishName;
    }

    public Map<String, SchedulerResult> getAlgorithmResults() {
        return algorithmResults;
    }
}

