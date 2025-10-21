package com.example.robotdelivery.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;
//单次磁盘调用算法的运行结果 便于前端可视化展示
@Data
public class SchedulerResult {
    private String algorithmName;        // 算法名称（FCFS/SSTF/SCAN）
    private List<Integer> processedOrder;// 调度后的处理顺序（食材位置）
    private List<Integer> stepDistances; // 每一步移动距离
    private int totalDistance;           // 总移动距离
    private List<String> stepDetails;    // 步骤描述
    private Map<Integer, String> warehouseIngredients; // 仓库食材分布
    // 添加以下 setter 方法
    public void setWarehouseIngredients(Map<Integer, String> warehouseIngredients) {
        this.warehouseIngredients = warehouseIngredients;
    }

    public void setAlgorithmName(String algorithmName) {
        this.algorithmName = algorithmName;
    }

    public void setProcessedOrder(List<Integer> processedOrder) {
        this.processedOrder = processedOrder;
    }

    public void setStepDistances(List<Integer> stepDistances) {
        this.stepDistances = stepDistances;
    }

    public void setTotalDistance(int totalDistance) {
        this.totalDistance = totalDistance;
    }

    public void setStepDetails(List<String> stepDetails) {
        this.stepDetails = stepDetails;
    }
    public int getTotalDistance() {
        return totalDistance;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public List<Integer> getProcessedOrder() {
        return processedOrder;
    }

    public List<String> getStepDetails() {
        return stepDetails;
    }

    public List<Integer> getStepDistances() {
        return stepDistances;
    }

    public Map<Integer, String> getWarehouseIngredients() {
        return warehouseIngredients;
    }
}

