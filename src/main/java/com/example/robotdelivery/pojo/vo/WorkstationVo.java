package com.example.robotdelivery.pojo.vo;

import com.example.robotdelivery.pojo.Memory;
import com.example.robotdelivery.pojo.Partition;

import java.util.List;

/**
 * 工作台表格视图对象 (用于第二个前端表格)
 * 对应表格的每一行数据。
 */
public class WorkstationVo {
    private int id;                 // 工作台ID (对应 Partition ID)
    private int capacity;           // 容量 (对应 Partition size)
    private String status;          // 状态 (e.g., "空闲", "已分配")
    private String occupiedByRobot; // 占用机器人 (e.g., "Robot-1", "无")
    private String currentTask;     // 当前任务 (e.g., "Dish-101", "无")

    // 静态工厂方法：仅通过 Memory 对象构建 WorkstationVo（需指定当前分区索引）
    public static WorkstationVo fromMemory(Memory memory, int partitionIndex) {
        WorkstationVo vo = new WorkstationVo();
        // 从 Memory 中获取指定索引的分区
        List<Partition> partitions = memory.getPartitions();
        if (partitionIndex < 0 || partitionIndex >= partitions.size()) {
            throw new IllegalArgumentException("分区索引无效: " + partitionIndex);
        }
        Partition partition = partitions.get(partitionIndex);

        // 1. 基础信息（来自分区）
        vo.setId(partition.getId());
        vo.capacity = partition.getSize();

        // 2. 状态和任务（来自分区）
        if (partition.isAllocated()) {
            vo.status = "已分配";
            vo.currentTask = "Order-" + partition.getOrderId();
        } else {
            vo.status = "空闲";
            vo.currentTask = "无";
        }

        // 3. 占用机器人（来自 Memory）
        Integer robotId = memory.getOccupiedByRobotId();
        if (robotId != null && robotId > 0) {
            vo.occupiedByRobot = "Robot-" + robotId;
        } else {
            vo.occupiedByRobot = "无";
        }

        return vo;
    }

    // Getters and Setters（保持不变）
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOccupiedByRobot() { return occupiedByRobot; }
    public void setOccupiedByRobot(String occupiedByRobot) { this.occupiedByRobot = occupiedByRobot; }
    public String getCurrentTask() { return currentTask; }
    public void setCurrentTask(String currentTask) { this.currentTask = currentTask; }
}