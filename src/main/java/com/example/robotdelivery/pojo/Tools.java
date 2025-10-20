package com.example.robotdelivery.pojo;

import java.util.concurrent.locks.ReentrantLock;

public class Tools
{
  // 工具类型枚举（文档“资源种类动态变化”需求）
    public enum ToolType {
        OVEN, // 烤箱
        FRY_PAN, // 煎锅
        FRY_POT
    }
    private Integer toolId; // 工具ID
    //private String toolName; // 工具名称，**改为了toolType，使用枚举
    private ToolType toolType; // 工具类型
    private Integer toolStatus = 0; // 当前占用状态，0表示空闲，>0表示被对应机器人占用
     private Integer occupiedByRobotId; // 绑定机器人（核心单元，非订单）
    private final ReentrantLock lock = new ReentrantLock(); // 工具锁，保证线程安全
  
     // 常量：状态定义
    public static final Integer STATUS_FREE = 0;
    public static final Integer STATUS_OCCUPIED = 1;

    public void setToolStatus(int x)
    {
        this.toolStatus=x;
    }


    public Integer getToolId()
    {
        return toolId;
    }

    public void setToolId(Integer toolId)
    {
        this.toolId = toolId;
    }

//     public String getToolName()
//     {
//         return toolName;
//     }

//     public void setToolName(String toolName)
//     {
//         this.toolName = toolName;
//     }

//     public Integer getToolStatus()
//     {
//         return toolStatus;
// >>>>>>> main
//     }

    // 尝试占用工具，成功返回true，失败返回false
    public boolean acquire(Integer robotId)
    {
        if (lock.tryLock())
        {
            this.toolStatus = robotId;
            return true;
        }
        return false;
    }

    // 释放工具，仅占用者可以释放
    public void release(Integer robotId)
    {
        if (lock.isHeldByCurrentThread() && this.toolStatus.equals(robotId))
        {
            this.toolStatus = 0;
            lock.unlock();
        }
    }

    public ToolType getToolType() {
        return toolType;
    }

    public void setToolType(ToolType toolType) {
        this.toolType = toolType;
    }

    public Integer getToolStatus() {
        return toolStatus;
    }

    public void setToolStatus(Integer toolStatus) {
        this.toolStatus = toolStatus;
    }

    public Integer getOccupiedByRobotId() {
        return occupiedByRobotId;
    }

    public void setOccupiedByRobotId(Integer occupiedByRobotId) {
        this.occupiedByRobotId = occupiedByRobotId;
    }
}