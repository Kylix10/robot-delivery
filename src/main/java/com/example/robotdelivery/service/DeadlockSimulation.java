package com.example.robotdelivery.service;

import com.example.robotdelivery.pojo.Order;
import com.example.robotdelivery.pojo.Dish;
import com.example.robotdelivery.pojo.Robot;
import com.example.robotdelivery.pojo.Tools;
import com.example.robotdelivery.pojo.Tools.ToolType;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;

/**
 * 用于模拟死锁可能性的对比测试
 * 不考虑优先级，每个订单都有权利申请资源
 */
public class DeadlockSimulation
{
    // 新增：线程安全的静态列表，存默认模式已完成的订单（供性能服务读取）
    public static final java.util.concurrent.CopyOnWriteArrayList<Order> COMPLETED_ORDERS = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final List<Order> orders;
    private final List<Robot> robots;
    private final List<Tools> tools;
    private final Random random = new Random();
    private static final long POLL_INTERVAL = 100; // ms

    public DeadlockSimulation(List<Order> orders)
    {
        // 深拷贝订单
        this.orders = deepCopyOrders(orders);

        // 初始化独立机器人和工具
        this.robots = initRobots();
        this.tools = initTools();
    }

    /** 深拷贝订单列表，保证每个订单和Dish都是全新对象 */
    public static List<Order> deepCopyOrders(List<Order> originalOrders)
    {
        List<Order> copied = new ArrayList<>();
        for (Order o : originalOrders)
        {
            if (o == null || o.getDish() == null)
            {
                continue;
            }

            Dish origDish = o.getDish();
            Dish newDish = new Dish();
            newDish.setDishId(origDish.getDishId());
            newDish.setDishName(origDish.getDishName());
            newDish.setRequiredSpace(origDish.getRequiredSpace());
            newDish.setNeedOven(origDish.getNeedOven());
            newDish.setNeedFryPan(origDish.getNeedFryPan());
            newDish.setNeedFryPot(origDish.getNeedFryPot());
            newDish.setCookTime(origDish.getCookTime());
            newDish.setDish_price(origDish.getDish_price());

            Order newOrder = new Order();
            newOrder.setOrderId(o.getOrderId());
            newOrder.setDish(newDish);
            newOrder.setCreateTime(o.getCreateTime());
            newOrder.setPriority(o.getPriority());

            copied.add(newOrder);
        }
        return copied;
    }

    private List<Robot> initRobots()
    {
        List<Robot> list = new ArrayList<>();
        Robot r1 = new Robot();
        r1.setRobotId(1);
        r1.setRobotStatus(Robot.STATUS_FREE);
        list.add(r1);

        Robot r2 = new Robot();
        r2.setRobotId(2);
        r2.setRobotStatus(Robot.STATUS_FREE);
        list.add(r2);

        return list;
    }

    private List<Tools> initTools()
    {
        List<Tools> list = new ArrayList<>();
        AtomicInteger toolCounter = new AtomicInteger(1);

        // 2烤箱
        for (int i = 0; i < 2; i++)
        {
            Tools t = new Tools();
            t.setToolId(toolCounter.getAndIncrement());
            t.setToolType(ToolType.OVEN);
            t.setToolStatus(Tools.STATUS_FREE);
            list.add(t);
        }

        // 2煎锅
        for (int i = 0; i < 2; i++)
        {
            Tools t = new Tools();
            t.setToolId(toolCounter.getAndIncrement());
            t.setToolType(ToolType.FRY_PAN);
            t.setToolStatus(Tools.STATUS_FREE);
            list.add(t);
        }

        // 1炸锅
        Tools t = new Tools();
        t.setToolId(toolCounter.getAndIncrement());
        t.setToolType(ToolType.FRY_POT);
        t.setToolStatus(Tools.STATUS_FREE);
        list.add(t);

        return list;
    }

    /** 启动模拟 */
    public void runSimulation()
    {
        System.out.println("=== DeadlockSimulation 启动 ===");

        boolean allDone;
        do
        {
            allDone = true;
            for (Order o : orders)
            {
                if (o.getCompleteTime() != null)
                {
                    continue;
                }
                allDone = false;
                attemptOrder(o);
            }

            try
            {
                Thread.sleep(POLL_INTERVAL);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }

        } while (!allDone);

        System.out.println("=== DeadlockSimulation 完成 ===");
        printResults();
    }

    private void attemptOrder(Order order)
    {
        Robot freeRobot = robots.stream()
                .filter(r -> r.getRobotStatus() == Robot.STATUS_FREE)
                .findFirst()
                .orElse(null);

        if (freeRobot == null)
        {
            return;
        }

        Dish dish = order.getDish();
        List<Tools> allocated = new ArrayList<>();

        if (dish.getNeedOven())
        {
            Tools oven = tools.stream()
                    .filter(t -> t.getToolType() == ToolType.OVEN && t.getToolStatus() == Tools.STATUS_FREE)
                    .findFirst()
                    .orElse(null);

            if (oven == null)
            {
                return;
            }

            oven.setToolStatus(Tools.STATUS_OCCUPIED);
            allocated.add(oven);
        }

        if (dish.getNeedFryPan())
        {
            Tools pan = tools.stream()
                    .filter(t -> t.getToolType() == ToolType.FRY_PAN && t.getToolStatus() == Tools.STATUS_FREE)
                    .findFirst()
                    .orElse(null);

            if (pan == null)
            {
                releaseTools(allocated);
                return;
            }

            pan.setToolStatus(Tools.STATUS_OCCUPIED);
            allocated.add(pan);
        }

        if (dish.getNeedFryPot() != null && dish.getNeedFryPot())
        {
            Tools pot = tools.stream()
                    .filter(t -> t.getToolType() == ToolType.FRY_POT && t.getToolStatus() == Tools.STATUS_FREE)
                    .findFirst()
                    .orElse(null);

            if (pot == null)
            {
                releaseTools(allocated);
                return;
            }

            pot.setToolStatus(Tools.STATUS_OCCUPIED);
            allocated.add(pot);
        }

        freeRobot.setRobotStatus(Robot.STATUS_BUSY);

        try
        {
            Thread.sleep(dish.getCookTime());
            order.setCompleteTime(LocalDateTime.now());
            Thread.sleep(dish.getCookTime());
            //新增 将完成的订单加入静态列表
            COMPLETED_ORDERS.add(order);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }

        freeRobot.setRobotStatus(Robot.STATUS_FREE);
        releaseTools(allocated);
    }

    private void releaseTools(List<Tools> allocated)
    {
        for (Tools t : allocated)
        {
            t.setToolStatus(Tools.STATUS_FREE);
        }
    }

    private void printResults()
    {
        System.out.println("=== DeadlockSimulation 订单完成情况 ===");
        for (Order o : orders)
        {
            System.out.println("订单ID: " + o.getOrderId() + " 菜品: " + o.getDish().getDishName() +
                    " 完成时间: " + o.getCompleteTime());
        }
    }

}
