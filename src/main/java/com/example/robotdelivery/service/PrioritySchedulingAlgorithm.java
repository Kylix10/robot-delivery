package com.example.robotdelivery.service;

import com.example.robotdelivery.pojo.Order;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 优先级调度算法
 * 对 ResourceManagerThread 的 orderWaitQueue 进行优先级排序
 */
public class PrioritySchedulingAlgorithm
{
    private final BlockingQueue<Order> orderQueue; // 阻塞队列引用
    private final ReentrantLock lock = new ReentrantLock(); // 队列锁

    public PrioritySchedulingAlgorithm(BlockingQueue<Order> orderQueue)
    {
        this.orderQueue = orderQueue;
    }

    /**
     * 对队列进行优先级排序（降序），同优先级保持 FIFO
     */
    public void sortQueue()
    {
        lock.lock();
        try
        {
            if (orderQueue.isEmpty()) return;

            // 1. 取出所有元素到临时列表
            List<Order> tempList = new ArrayList<>();
            orderQueue.drainTo(tempList);
            /*
            // 2. 更新优先级（这里可使用简单示例，实际可用计时器逻辑）
            for (Order order : tempList)
            {
                int x = 0; // 暂时赋值0
                order.updatePriority(x);
            }
            */
            // 3. 按优先级降序，同优先级保持FIFO（orderId升序）
            Collections.sort(tempList, Comparator
                    .comparing(Order::getPriority).reversed()
                    .thenComparing(Order::getOrderId));

            // 4. 放回队列
            orderQueue.addAll(tempList);
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * 打印当前队列状态（优先级降序）
     */
    public void printQueue()
    {
        lock.lock();
        try
        {
            if (orderQueue.isEmpty())
            {
                System.out.println("===== 队列为空 =====");
                return;
            }
            System.out.println("===== 当前订单队列（优先级降序） =====");
            for (Order order : orderQueue)
            {
                String info = "订单ID: " + order.getOrderId() +
                        ", 菜品: " + (order.getDish() != null ? order.getDish().getDishName() : "无") +
                        ", 优先级: " + order.getPriority();
                System.out.println(info);
            }
            System.out.println("=====================================");
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * 清空队列（测试或重置用）
     */
    public void clearQueue()
    {
        lock.lock();
        try
        {
            orderQueue.clear();
        }
        finally
        {
            lock.unlock();
        }
    }
}
