package com.example.robotdelivery.service;

import com.example.robotdelivery.mapper.RobotRepository;

import com.example.robotdelivery.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Random;

@Service
public class ResourceManagerThread extends Thread {
    @Autowired
    private BankerAlgorithm bankerAlgorithm;

    // 新增：注入机器人Repository（操作数据库）
    @Autowired
    private RobotRepository robotRepository;

    // 初始化资源：2烤箱+1煎锅+2机器人+1工作区（贴合文档资源定义）
    private final List<Tools> allTools = initTools();
    private final List<Robot> allRobots = initRobots();
    private final Memory workbench = new Memory();
    private final BlockingQueue<Order> orderWaitQueue = new LinkedBlockingQueue<>();

    // 需要在类中定义这三个菜品变量
    private Dish dishA;
    private Dish dishB;
    private Dish dishC;

    private long lastPrintTime = 0;
    private static final long PRINT_INTERVAL = 500; // 3秒间隔

    // 初始化三种不同菜品（A/B/C类型，按文档需求配置）
    private void initDishes() {
        // 使用Dish类的构造方法，按类型初始化三种菜品
        dishA = new Dish(Dish.DishType.A);
        dishB = new Dish(Dish.DishType.B);
        dishC = new Dish(Dish.DishType.C);

        // 可以在这里设置ID（如果需要与数据库交互）
        dishA.setDishId(1);
        dishB.setDishId(2);
        dishC.setDishId(3);
    }

    private void generateOrders() {
        // 先初始化三种菜品
        initDishes();

        // 准备菜品列表，用于随机分配
        List<Dish> dishes = Arrays.asList(dishA, dishB, dishC);
        Random random = new Random();

        // 循环生成 5 个订单
        for (int i = 1; i <= 5; i++) {
            Order order = new Order();
            order.setOrderId(i);

            // 随机选择一种菜品（0:A, 1:B, 2:C）
            Dish randomDish = dishes.get(random.nextInt(3));
            order.setDish(randomDish);

            order.setOrderStatus(Order.OrderStatus.PENDING);

            try {
                submitOrder(order);
                System.out.println("提交订单 " + i + "（菜品：" + randomDish.getDishName() +
                        "，需要烤箱：" + randomDish.getNeedOven() +
                        "，需要煎锅：" + randomDish.getNeedFryPan() +
                        "，所需空间：" + randomDish.getRequiredSpace() + "）");

                // 每个订单提交后休眠 0.5秒，实现间隔提交
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // 线程启动时提交单个订单，再处理订单
    @Override
    public void run() {


        System.out.println("资源管理线程启动，初始资源：2烤箱+1煎锅+2机器人+工作区100空间");
        // 调用生成多个间隔订单的方法
        generateOrders();

        // 控制循环速度的休眠时间（毫秒），可根据需要调整
        final long LOOP_DELAY = 1000; // 0.5秒，可改为1000（1秒）等

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Order order = orderWaitQueue.take();
                // 订单非空且菜品非空校验（双重保障）
                if (order == null || order.getDish() == null) {
                    System.out.println("跳过无效订单（订单或菜品为空）");
                    Thread.sleep(LOOP_DELAY); // 无效订单也延迟
                    continue;
                }
                Robot freeRobot = findFreeRobot();
                if (freeRobot == null) {
                    orderWaitQueue.offer(order);
                    System.out.println("无空闲机器人，订单" + order.getOrderId() + "放回等待队列");
                    Thread.sleep(LOOP_DELAY); // 无效订单也延迟
                    Thread.sleep(1000);
                    continue;
                }


                // 银行家算法检查（贴合文档死锁避免需求）
                boolean isSafe = bankerAlgorithm.isResourceSafe(
                        freeRobot,
                        order,
                        allTools.stream().filter(t -> t.getStatus() == Tools.STATUS_FREE).collect(Collectors.toList()), // 实时空闲工具
                        allRobots, // 传入真实机器人列表
                        workbench
                );

                if (isSafe) {
                    // 关键：仅当allocateResource返回true（分配成功），才启动制作线程
                    boolean allocateSuccess = allocateResource(freeRobot, order);
                    if(allocateSuccess) {
                        simulateOrderProcessing(freeRobot, order); // 分配成功才调用
                    }
                } else {
                    orderWaitQueue.offer(order);
                    System.out.println("资源不足/不安全，订单" + order.getOrderId() + "放回等待队列");
                    Thread.sleep(LOOP_DELAY); // 无效订单也延迟
                }
                printResourceStatus();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("资源管理线程停止");
                break;
            }
        }
    }

    // 以下为原有初始化工具、机器人、分配/释放资源等逻辑，保持不变
    private List<Tools> initTools() {
        List<Tools> tools = new ArrayList<>();
        // 用setter初始化工具，不使用有参构造
        Tools oven1 = new Tools();
        oven1.setToolId(1);
        oven1.setToolType(Tools.ToolType.OVEN);
        oven1.setStatus(Tools.STATUS_FREE);
        tools.add(oven1);

        Tools oven2 = new Tools();
        oven2.setToolId(2);
        oven2.setToolType(Tools.ToolType.OVEN);
        oven2.setStatus(Tools.STATUS_FREE);
        tools.add(oven2);

        Tools fryPan = new Tools();
        fryPan.setToolId(3);
        fryPan.setToolType(Tools.ToolType.FRY_PAN);
        fryPan.setStatus(Tools.STATUS_FREE);
        tools.add(fryPan);
        return tools;
    }

    private List<Robot> initRobots() {
        List<Robot> robots = new ArrayList<>();
        // 用setter初始化机器人，不使用有参构造
        Robot robot1 = new Robot();
        robot1.setRobotId(1);
        robot1.setRobotStatus(Robot.STATUS_FREE);
        robots.add(robot1);

        Robot robot2 = new Robot();
        robot2.setRobotId(2);
        robot2.setRobotStatus(Robot.STATUS_FREE);
        robots.add(robot2);
        return robots;
    }

    public void submitOrder(Order order) {
        try {
            // 增加dish非空校验，避免无效订单进入队列
            if (order == null || order.getDish() == null) {
                System.out.println("拒绝提交无效订单：order或dish为null");
                return; // 直接返回，不提交空订单
            }
            orderWaitQueue.put(order);
            System.out.println("订单" + order.getOrderId() + "（菜品：" + order.getDish().getDishName() + "）提交成功");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean allocateResource(Robot robot, Order order) {
        Dish dish = order.getDish();
        // 记录已分配的资源，用于回滚
        List<Tools> allocatedTools = new ArrayList<>();
        boolean workspaceAllocated = false;
        int originalWorkspaceUsed = workbench.getUsedSpace(); // 记录原始工作区已用空间

        try {
            // 1. 检查并分配工作区
            if (workbench.getFreeSpace() < dish.getRequiredSpace()) {
                System.out.println("工作区空间不足，订单" + order.getOrderId() + "放回等待队列");
                orderWaitQueue.offer(order);
                return false;
            }
            // 预分配工作区（先占用，后续失败需回滚）
            workbench.setUsedSpace(workbench.getUsedSpace() + dish.getRequiredSpace());
            workbench.setOccupiedByRobotId(robot.getRobotId());
            robot.setOccupiedWorkbench(workbench);
            workspaceAllocated = true;
            System.out.println("工作区预分配成功，已用空间: " + workbench.getUsedSpace());

            // 2. 检查并分配烤箱
            if (dish.getNeedOven()) {
                List<Tools> freeOvens = allTools.stream()
                        .filter(t -> t.getToolType() == Tools.ToolType.OVEN && t.getStatus() == Tools.STATUS_FREE)
                        .collect(Collectors.toList());
                if (freeOvens.isEmpty()) {
                    System.out.println("无空闲烤箱，订单" + order.getOrderId() + "放回等待队列");
                    orderWaitQueue.offer(order);
                    // 回滚已分配资源
                    rollbackResources(allocatedTools, robot, workspaceAllocated, originalWorkspaceUsed);
                    return false;
                }
                // 分配烤箱
                Tools oven = freeOvens.get(0);
                oven.setStatus(Tools.STATUS_OCCUPIED);
                oven.setOccupiedByRobotId(robot.getRobotId());
                robot.setOccupiedOven(oven);
                allocatedTools.add(oven);
                System.out.println("烤箱" + oven.getToolId() + "预分配成功");
            }

            // 3. 检查并分配煎锅
            if (dish.getNeedFryPan()) {
                List<Tools> freeFryPans = allTools.stream()
                        .filter(t -> t.getToolType() == Tools.ToolType.FRY_PAN && t.getStatus() == Tools.STATUS_FREE)
                        .collect(Collectors.toList());
                if (freeFryPans.isEmpty()) {
                    System.out.println("无空闲煎锅，订单" + order.getOrderId() + "放回等待队列");
                    orderWaitQueue.offer(order);
                    // 回滚已分配资源
                    rollbackResources(allocatedTools, robot, workspaceAllocated, originalWorkspaceUsed);
                    return false;
                }
                // 分配煎锅
                Tools fryPan = freeFryPans.get(0);
                fryPan.setStatus(Tools.STATUS_OCCUPIED);
                fryPan.setOccupiedByRobotId(robot.getRobotId());
                robot.setOccupiedFryPan(fryPan);
                allocatedTools.add(fryPan);
                System.out.println("煎锅" + fryPan.getToolId() + "预分配成功");
            }

            // 4. 所有资源分配成功，更新最终状态
            order.setOrderStatus(Order.OrderStatus.PROCESSING);
            robot.setRobotStatus(Robot.STATUS_BUSY);
            robot.setCurrentOrder(order);

            System.out.println("机器人" + robot.getRobotId() + "分配资源成功，处理订单" + order.getOrderId());
            return true;

        } catch (Exception e) {
            System.err.println("资源分配异常，触发回滚: " + e.getMessage());
            // 异常情况下回滚所有已分配资源
            rollbackResources(allocatedTools, robot, workspaceAllocated, originalWorkspaceUsed);
            return false;
        }
    }

    /**
     * 回滚所有已分配的资源（工具和工作区）
     * @param allocatedTools 已分配的工具列表
     * @param robot 执行分配的机器人
     * @param workspaceAllocated 工作区是否已分配
     * @param originalWorkspaceUsed 原始工作区已用空间
     */
    private void rollbackResources(List<Tools> allocatedTools, Robot robot,
                                   boolean workspaceAllocated, int originalWorkspaceUsed) {
        // 1. 回滚工具资源
        if (!allocatedTools.isEmpty()) {
            for (Tools tool : allocatedTools) {
                tool.setStatus(Tools.STATUS_FREE);
                tool.setOccupiedByRobotId(null);
                // 解除机器人与工具的关联
                if (tool.getToolType() == Tools.ToolType.OVEN) {
                    robot.setOccupiedOven(null);
                } else if (tool.getToolType() == Tools.ToolType.FRY_PAN) {
                    robot.setOccupiedFryPan(null);
                }
                System.out.println("已回滚工具: " + tool.getToolType() + "(ID:" + tool.getToolId() + ")");
            }
        }

        // 2. 回滚工作区资源
        if (workspaceAllocated) {
            workbench.setUsedSpace(originalWorkspaceUsed);
            workbench.setOccupiedByRobotId(null);
            robot.setOccupiedWorkbench(null);
            System.out.println("已回滚工作区，恢复已用空间为: " + originalWorkspaceUsed);
        }

        // 3. 重置机器人状态
        robot.setRobotStatus(Robot.STATUS_FREE);
        robot.setCurrentOrder(null);
    }


    private void releaseResource(Robot robot) {
        System.out.println("开始释放机器人" + robot.getRobotId() + "的资源"); // 新增日志
        Order order = robot.getCurrentOrder();
        if (order == null) return;
        Dish dish = order.getDish();
        if (robot.getOccupiedOven() != null) {
            Tools oven = robot.getOccupiedOven();
            oven.setStatus(Tools.STATUS_FREE);
            oven.setOccupiedByRobotId(null);
            robot.setOccupiedOven(null);
        }
        if (robot.getOccupiedFryPan() != null) {
            Tools fryPan = robot.getOccupiedFryPan();
            fryPan.setStatus(Tools.STATUS_FREE);
            fryPan.setOccupiedByRobotId(null);
            robot.setOccupiedFryPan(null);
        }
        // 关键：释放工作区空间
        int originalUsed = workbench.getUsedSpace();
        workbench.setUsedSpace(Math.max(originalUsed - dish.getRequiredSpace(), 0));
        workbench.setOccupiedByRobotId(null);
        System.out.println("释放工作区空间：原已用" + originalUsed + "→新已用" + workbench.getUsedSpace());

        robot.setOccupiedWorkbench(null);
        order.setOrderStatus(Order.OrderStatus.COMPLETED);
        robot.setCurrentOrder(null);
        robot.setRobotStatus(Robot.STATUS_FREE);

        System.out.println("机器人" + robot.getRobotId() + "释放资源，订单" + order.getOrderId() + "完成");
    }

    private void simulateOrderProcessing(Robot robot, Order order) {
        new Thread(() -> {
            try {
                System.out.println("订单开始制作:"+order.getOrderId());
                Thread.sleep(50);
                System.out.println("订单制作时间到:"+order.getOrderId());
                releaseResource(robot);
                printResourceStatus();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private Robot findFreeRobot() {
        return allRobots.stream()
                .filter(robot -> robot.getRobotStatus() == Robot.STATUS_FREE)
                .findFirst().orElse(null);
    }

    private void printResourceStatus() {
        long currentTime = System.currentTimeMillis();
        // 若距离上次打印不足3秒，则跳过
        if (currentTime - lastPrintTime < PRINT_INTERVAL) {
            return;
        }
        lastPrintTime = currentTime; // 更新上次打印时间

        System.out.println("\n===== 当前资源状态 =====");
        System.out.println("1. 工具状态：");
        for (Tools tool : allTools) {
            String status = tool.getStatus() == Tools.STATUS_FREE ? "空闲" : "被机器人" + tool.getOccupiedByRobotId() + "占用";
            System.out.println("   " + tool.getToolType() + "（ID：" + tool.getToolId() + "）：" + status);
        }
        System.out.println("2. 工作区状态：");
        System.out.println("   总空间：" + workbench.getTotalSpace() + "，已用：" + workbench.getUsedSpace() + "，空闲：" + workbench.getFreeSpace());
        String workbenchOccupy = workbench.getOccupiedByRobotId() == null ? "空闲" : "被机器人" + workbench.getOccupiedByRobotId() + "占用";
        System.out.println("   占用情况：" + workbenchOccupy);
        System.out.println("3. 机器人状态：");
        for (Robot robot : allRobots) {
            String orderInfo = robot.getCurrentOrder() == null ? "无" : "订单" + robot.getCurrentOrder().getOrderId() + "（菜品：" + robot.getCurrentOrder().getDish().getDishName() + "）";
            System.out.println("   机器人" + robot.getRobotId() + "：" + (robot.getRobotStatus() == Robot.STATUS_FREE ? "空闲" : "忙碌（处理" + orderInfo + "）"));
        }
        System.out.println("4. 订单等待队列：" + orderWaitQueue.size() + "个订单");
        System.out.println("=======================\n");
    }
}