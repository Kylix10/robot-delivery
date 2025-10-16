package com.example.robotdelivery.pojo;

/**
 * 工作区快照类：仅用于银行家算法的模拟分配，不影响真实Memory数据
 * 存放位置：与Memory类同目录（pojo包下）
 */
public class MemorySnapshot {
    // 与真实Memory对应的字段（仅包含模拟分配需要的属性）
    private int totalSpace;         // 总空间（与真实工作区一致）
    private int usedSpace;          // 模拟分配后的已用空间
    private Integer occupiedByRobotId; // 模拟占用的机器人ID

    /**
     * 从真实Memory创建快照（复制当前真实数据）
     * @param realMemory 真实工作区对象
     */
    public MemorySnapshot(Memory realMemory) {
        // 复制真实数据（保证初始状态与真实工作区一致）
        this.totalSpace = realMemory.getTotalSpace();
        this.usedSpace = realMemory.getUsedSpace();
        this.occupiedByRobotId = realMemory.getOccupiedByRobotId();
    }

    // 以下为getter和setter（仅提供模拟分配需要的方法）
    public int getTotalSpace() {
        return totalSpace;
    }

    public int getUsedSpace() {
        return usedSpace;
    }

    public void setUsedSpace(int usedSpace) {
        // 限制已用空间范围（0 ~ 总空间），避免模拟分配时出现异常值
        this.usedSpace = Math.max(0, Math.min(usedSpace, totalSpace));
    }

    public Integer getOccupiedByRobotId() {
        return occupiedByRobotId;
    }

    public void setOccupiedByRobotId(Integer occupiedByRobotId) {
        this.occupiedByRobotId = occupiedByRobotId;
    }

    // 计算模拟状态下的空闲空间（供银行家算法判断使用）
    public int getFreeSpace() {
        return totalSpace - usedSpace;
    }
}
