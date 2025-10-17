package com.example.robotdelivery.pojo.vo;

import com.example.robotdelivery.pojo.Partition;
import java.util.List;

/**
 * 内存/工作台状态可视化视图对象
 * 封装了总览信息和所有分区列表，用于前端展示。
 */
public class MemoryVO {

    // 1. 整体工作台信息（来自 Memory POJO）
    private int totalSpace;     // 总空间
    private int usedSpace;      // 已使用空间
    private int freeSpace;      // 空闲空间

    // 2. 分区列表（来自 MemoryManager）
    // Partition POJO 可以直接复用，因为它已经包含了所有必要信息
    private List<Partition> partitions;

    // Getters and Setters
    public int getTotalSpace() {
        return totalSpace;
    }

    public void setTotalSpace(int totalSpace) {
        this.totalSpace = totalSpace;
    }

    public int getUsedSpace() {
        return usedSpace;
    }

    public void setUsedSpace(int usedSpace) {
        this.usedSpace = usedSpace;
    }

    public int getFreeSpace() {
        return freeSpace;
    }

    public void setFreeSpace(int freeSpace) {
        this.freeSpace = freeSpace;
    }

    public List<Partition> getPartitions() {
        return partitions;
    }

    public void setPartitions(List<Partition> partitions) {
        this.partitions = partitions;
    }

    @Override
    public String toString() {
        return "MemoryVO{" +
                "totalSpace=" + totalSpace +
                ", usedSpace=" + usedSpace +
                ", freeSpace=" + freeSpace +
                ", partitionCount=" + (partitions != null ? partitions.size() : 0) +
                '}';
    }
}