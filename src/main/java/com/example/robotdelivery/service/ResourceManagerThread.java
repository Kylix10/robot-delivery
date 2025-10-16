package com.example.robotdelivery.service;

import com.example.robotdelivery.pojo.Robot;
import com.example.robotdelivery.pojo.Tools;

import java.util.List;

// 2025.10.16 17：00  实现了工具的互斥锁，用于解决哲学家就餐问题，需要等待银行家算法部分写完后，商量一下两者的接口问题


public class ResourceManagerThread extends Thread
{
    private Robot robot;
    private List<Tools> requiredTools;

    public ResourceManagerThread(Robot robot, List<Tools> requiredTools)
    {
        this.robot = robot;
        this.requiredTools = requiredTools;
    }

    @Override
    public void run()
    {
        while (!Thread.currentThread().isInterrupted())
        {
            boolean allAcquired = tryAcquireAllTools();

            if (allAcquired)
            {
                // 执行任务
                performTask();

                // 释放工具
                releaseAllTools();

                break; // 完成任务后退出线程
            }
            else
            {
                // 没拿到所有工具，稍微等待再重试
                try
                {
                    Thread.sleep(50); // 等待50ms后重试
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private boolean tryAcquireAllTools()
    {
        for (Tools tool : requiredTools)
        {
            boolean acquired = tool.acquire(robot.getRobotId());

            // 银行家算法接口占位（等银行家算法写完再实现）
            // boolean safe = BankerAlgorithm.checkAllocation(robot, tool);
            // if (!safe) { tool.release(robot.getRobotId()); return false; }

            if (!acquired)
            {
                // 如果某个工具没拿到，已经拿到的工具需要释放
                releasePartialTools();
                return false;
            }
        }
        return true;
    }

    private void releasePartialTools()
    {
        for (Tools tool : requiredTools)
        {
            if (tool.getToolStatus().equals(robot.getRobotId()))
            {
                tool.release(robot.getRobotId());
            }
        }
    }

    private void releaseAllTools()
    {
        for (Tools tool : requiredTools)
        {
            tool.release(robot.getRobotId());
        }
    }

    private void performTask()
    {
        // 执行机器人操作逻辑
        // TODO: 这里可以调用机器人工作方法
    }
}
