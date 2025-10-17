package com.example.robotdelivery.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class SchedulerResult {
    private String algorithmName;        // 算法名称（FCFS/SSTF/SCAN）
    private List<Integer> processedOrder;// 调度后的处理顺序（食材位置）
    private List<Integer> stepDistances; // 每一步移动距离
    private int totalDistance;           // 总移动距离
    private List<String> stepDetails;    // 步骤描述
    private Map<Integer, String> warehouseIngredients; // 仓库食材分布
}

