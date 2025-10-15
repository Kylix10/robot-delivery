package com.example.robotdelivery.pojo;

/**
 * 工作区资源（总空间100）
 * 对应文档实验三“动态分区算法”，模拟内存分配
 */
public class Memory {

    private Integer workbenchId = 1;
    private Integer totalSpace = 100; // 固定总空间（你设定的值）
    private Integer usedSpace = 0; // 已用空间
    private Integer freeSpace = 100; // 空闲空间（自动计算）
    private Integer occupiedByRobotId; // 绑定机器人（核心单元）

    // Getter 和 Setter 方法
    public Integer getWorkbenchId() {
        return workbenchId;
    }

    public void setWorkbenchId(Integer workbenchId) {
        this.workbenchId = workbenchId;
    }

    public Integer getTotalSpace() {
        return totalSpace;
    }

    public void setTotalSpace(Integer totalSpace) {
        this.totalSpace = totalSpace;
    }

    public Integer getUsedSpace() {
        return usedSpace;
    }

    public void setUsedSpace(Integer usedSpace) {
        this.usedSpace = usedSpace;
        this.freeSpace = this.totalSpace - usedSpace;
    }

    public Integer getFreeSpace() {
        return freeSpace;
    }

    public Integer getOccupiedByRobotId() {
        return occupiedByRobotId;
    }

    public void setOccupiedByRobotId(Integer occupiedByRobotId) {
        this.occupiedByRobotId = occupiedByRobotId;
    }
}