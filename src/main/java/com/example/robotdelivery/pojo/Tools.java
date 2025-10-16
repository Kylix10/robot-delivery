package com.example.robotdelivery.pojo;

import java.util.concurrent.locks.ReentrantLock;

public class Tools
{
    private Integer toolId; // 工具ID
    private String toolName; // 工具名称
    private Integer toolStatus = 0; // 当前占用状态，0表示空闲，>0表示被对应机器人占用
    private final ReentrantLock lock = new ReentrantLock(); // 工具锁，保证线程安全

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

    public String getToolName()
    {
        return toolName;
    }

    public void setToolName(String toolName)
    {
        this.toolName = toolName;
    }

    public Integer getToolStatus()
    {
        return toolStatus;
    }

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
}
