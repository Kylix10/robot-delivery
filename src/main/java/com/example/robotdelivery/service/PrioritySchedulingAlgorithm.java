package com.example.robotdelivery.service;

import com.example.robotdelivery.pojo.Order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

//2025.10.16 17：00 实现了优先级排序，提供了优先级更新的方法但未具体实现，需要讨论计时器相关问题



// 优先级排序
public class PrioritySchedulingAlgorithm
{

    private static final List<Order> globalOrderQueue = new ArrayList<>(); // 全局订单队列
    private static final ReentrantLock lock = new ReentrantLock();         // 队列锁

    /**
     * 将新的订单列表加入全局队列，并按优先级排序
     * @param newOrders 新生成的订单列表
     */
    public static void addAndSortOrders(List<Order> newOrders)
    {
        if (newOrders == null || newOrders.isEmpty())
        {
            return;
        }

        lock.lock();
        try
        {
            globalOrderQueue.addAll(newOrders); // 加入全局队列

            // 更新所有订单优先级
            for (Order order : globalOrderQueue)
            {
                int x;
                x = 0; // 暂时赋值为0，计算逻辑可用计时器实现
                // x = (int)((System.currentTimeMillis() - order.getCreateTime().getTime()) / 60000); // 每分钟增加一次优先级，暂时注释
                order.updatePriority(x);
            }

            // 按优先级降序排序，同优先级保持 FIFO（orderId 升序）
            Collections.sort(globalOrderQueue, Comparator
                    .comparing(Order::getPriority).reversed()
                    .thenComparing(Order::getOrderId));
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * 获取当前全局订单队列（线程安全，返回原始队列）
     * @return 订单队列
     */
    public static List<Order> getGlobalOrderQueue()
    {
        lock.lock();
        try
        {
            return globalOrderQueue; // 返回原始队列，允许修改
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * 打印全局订单队列，用于调试
     */
    public static void printGlobalOrderQueue()
    {
        lock.lock();
        try
        {
            System.out.println("===== 全局订单队列（优先级降序） =====");
            for (Order order : globalOrderQueue)
            {
                System.out.println(order.toString());
            }
            System.out.println("=====================================");
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * 清空全局队列（测试或重置用）
     */
    public static void clearQueue()
    {
        lock.lock();
        try
        {
            globalOrderQueue.clear();
        }
        finally
        {
            lock.unlock();
        }
    }
}
