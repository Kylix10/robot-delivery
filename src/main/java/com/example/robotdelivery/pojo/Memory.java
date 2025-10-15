package com.example.robotdelivery.pojo;

/**
 * 工作区资源（总空间100）
 * 对应文档实验三“动态分区算法”，模拟内存分配
 */
public class Memory {

    private Integer workbenchId = 1;
    private final Integer totalSpace = 100; // 总空间固定，无需修改，用final
    private volatile Integer usedSpace = 0; // volatile保证多线程可见性
    private volatile Integer occupiedByRobotId; // volatile保证可见性

    // Getter 方法（无需同步，volatile已保证可见性）
    public Integer getWorkbenchId() {
        return workbenchId;
    }

    public Integer getTotalSpace() {
        return totalSpace;
    }

    public Integer getUsedSpace() {
        return usedSpace;
    }

    public Integer getOccupiedByRobotId() {
        return occupiedByRobotId;
    }

    // Setter 方法（synchronized保证原子性）
    public synchronized void setUsedSpace(Integer usedSpace) {
        this.usedSpace = Math.max(0, Math.min(usedSpace, totalSpace)); // 防止超界
        // freeSpace 由 usedSpace 推导，无需单独维护字段
    }

    public synchronized void setOccupiedByRobotId(Integer occupiedByRobotId) {
        this.occupiedByRobotId = occupiedByRobotId;
    }

    // 计算空闲空间（基于 usedSpace，无需单独字段）
    public synchronized Integer getFreeSpace() {
        return totalSpace - usedSpace;
    }
}