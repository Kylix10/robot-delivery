package com.example.robotdelivery.service;

import com.example.robotdelivery.mapper.RobotRepository;
import com.example.robotdelivery.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Random;
import java.time.LocalDateTime;

@Service
public class ResourceManagerThread extends Thread {
    @Autowired
    BankerAlgorithm bankerAlgorithm;

   // @Autowired
    //RobotRepository robotRepository;

    // 新增：注入OrderService
    //@Autowired
    //private OrderService orderService;

   // @Autowired
    //private TransactionTemplate transactionTemplate;

    private final List<Tools> allTools = initTools();
    //private List<Robot> allRobots;
    private final List<Robot> allRobots = initRobots();
    private final Memory workbench = new Memory();
    private final BlockingQueue<Order> orderWaitQueue = new LinkedBlockingQueue<>();

    private Dish dishA;
    private Dish dishB;
    private Dish dishC;

    private long lastPrintTime = 0;
    private static final long PRINT_INTERVAL = 500;

    public ResourceManagerThread() {
    }

//存数据库用
//    @PostConstruct
//    public void initRobots() {
//        this.allRobots = initAndSaveRobots();
//    }

    private void initDishes() {
        dishA = new Dish(Dish.DishType.A);
        dishB = new Dish(Dish.DishType.B);
        dishC = new Dish(Dish.DishType.C);

        dishA.setDishId(1);
        dishB.setDishId(2);
        dishC.setDishId(3);
    }

//    private void generateOrders() {
//        initDishes();
//
//        List<Dish> dishes = Arrays.asList(dishA, dishB, dishC);
//        Random random = new Random();
//
//        for (int i = 1; i <= 5; i++) {
//            Order order = new Order();
//            // 注意：不要手动设置orderId，由数据库自增生成（避免主键冲突）
//             order.setOrderId(i); // 存数据库的时候注释掉这行，改用数据库自增
//
//            Dish randomDish = dishes.get(random.nextInt(3));
//            order.setDish(randomDish);
//            order.setOrderStatus(Order.OrderStatus.PENDING); // 初始状态：待处理
//            order.setCreateTime(LocalDateTime.now());
//
//            try {
//                // 关键：调用OrderService保存订单到数据库
//                //Order savedOrder = orderService.saveOrder(order);
//                // 将保存后的订单（含数据库生成的orderId）放入等待队列
//                submitOrder(order);//参数变成saveOrder
//                System.out.println("提交订单 " + i + "（菜品：" + randomDish.getDishName() +
//                        "，需要烤箱：" + randomDish.getNeedOven() +
//                        "，需要煎锅：" + randomDish.getNeedFryPan() +
//                        "，所需空间：" + randomDish.getRequiredSpace() + "）");
//
//                Thread.sleep(500);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }
//    }

    @Override
    public void run() {
        System.out.println("资源管理线程启动，初始资源：2烤箱+1煎锅+2机器人+工作区100空间");

        final long LOOP_DELAY = 1000;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Order order = orderWaitQueue.take();
                if (order == null || order.getDish() == null) {
                    System.out.println("跳过无效订单（订单或菜品为空）");
                    Thread.sleep(LOOP_DELAY);
                    continue;
                }
                Robot freeRobot = findFreeRobot();
                if (freeRobot == null) {
                    orderWaitQueue.offer(order);
                    System.out.println("无空闲机器人，订单" + order.getOrderId() + "放回等待队列");
                    Thread.sleep(LOOP_DELAY);
                    continue;
                }

                boolean isSafe = bankerAlgorithm.isResourceSafe(
                        freeRobot,
                        order,
                        allTools.stream().filter(t -> t.getToolStatus() == Tools.STATUS_FREE).collect(Collectors.toList()),
                        allRobots,
                        workbench
                );

                if (isSafe) {
                    boolean allocateSuccess = allocateResource(freeRobot, order);
                    if (allocateSuccess) {
                        simulateOrderProcessing(freeRobot, order);
                    }
                } else {
                    orderWaitQueue.offer(order);
                    System.out.println("资源不足/不安全，订单" + order.getOrderId() + "放回等待队列");
                    Thread.sleep(LOOP_DELAY);
                }
                printResourceStatus();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("资源管理线程停止");
                break;
            } catch (Exception e) {
                System.err.println("资源管理线程发生未预期异常：" + e.getMessage());
                e.printStackTrace();
                try {
                    Thread.sleep(LOOP_DELAY);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private List<Tools> initTools() {
        List<Tools> tools = new ArrayList<>();
        Tools oven1 = new Tools();
        oven1.setToolId(1);
        oven1.setToolType(Tools.ToolType.OVEN);
        oven1.setToolStatus(Tools.STATUS_FREE);
        tools.add(oven1);

        Tools oven2 = new Tools();
        oven2.setToolId(2);
        oven2.setToolType(Tools.ToolType.OVEN);
        oven2.setToolStatus(Tools.STATUS_FREE);
        tools.add(oven2);

        Tools fryPan = new Tools();
        fryPan.setToolId(3);
        fryPan.setToolType(Tools.ToolType.FRY_PAN);
        fryPan.setToolStatus(Tools.STATUS_FREE);
        tools.add(fryPan);
        return tools;
    }
//存数据库用这个
//    private List<Robot> initAndSaveRobots() {
//        List<Robot> robots = new ArrayList<>();
//        List<Robot> existingRobots = robotRepository.findAll();
//        if (existingRobots.isEmpty()) {
//            Robot robot1 = new Robot();
//            robot1.setRobotId(1);
//            robot1.setRobotStatus(Robot.STATUS_FREE);
//            robots.add(robotRepository.save(robot1));
//
//            Robot robot2 = new Robot();
//            robot2.setRobotId(2);
//            robot2.setRobotStatus(Robot.STATUS_FREE);
//            robots.add(robotRepository.save(robot2));
//        } else {
//            robots.addAll(existingRobots);
//        }
//        return robots;
//    }

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
            if (order == null || order.getDish() == null) {
                System.out.println("拒绝提交无效订单：order或dish为null");
                return;
            }
            orderWaitQueue.put(order);
            System.out.println("订单" + order.getOrderId() + "（菜品：" + order.getDish().getDishName() + "）提交成功");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Transactional
    private boolean allocateResource(Robot robot, Order order) {
        Dish dish = order.getDish();
        List<Tools> allocatedTools = new ArrayList<>();
        boolean workspaceAllocated = false;
        int originalWorkspaceUsed = workbench.getUsedSpace();

        try {
            if (workbench.getFreeSpace() < dish.getRequiredSpace()) {
                System.out.println("工作区空间不足，订单" + order.getOrderId() + "放回等待队列");
                orderWaitQueue.offer(order);
                return false;
            }
            workbench.setUsedSpace(workbench.getUsedSpace() + dish.getRequiredSpace());
            workbench.setOccupiedByRobotId(robot.getRobotId());
            robot.setOccupiedWorkbench(workbench);
            workspaceAllocated = true;
            System.out.println("工作区预分配成功，已用空间: " + workbench.getUsedSpace());

            if (dish.getNeedOven()) {
                List<Tools> freeOvens = allTools.stream()
                        .filter(t -> t.getToolType() == Tools.ToolType.OVEN && t.getToolStatus() == Tools.STATUS_FREE)
                        .collect(Collectors.toList());
                if (freeOvens.isEmpty()) {
                    System.out.println("无空闲烤箱，订单" + order.getOrderId() + "放回等待队列");
                    orderWaitQueue.offer(order);
                    rollbackResources(allocatedTools, robot, workspaceAllocated, originalWorkspaceUsed);
                    return false;
                }
                Tools oven = freeOvens.get(0);
                oven.setToolStatus(Tools.STATUS_OCCUPIED);
                oven.setOccupiedByRobotId(robot.getRobotId());
                robot.setOccupiedOven(oven);
                allocatedTools.add(oven);
                System.out.println("烤箱" + oven.getToolId() + "预分配成功");
            }

            if (dish.getNeedFryPan()) {
                List<Tools> freeFryPans = allTools.stream()
                        .filter(t -> t.getToolType() == Tools.ToolType.FRY_PAN && t.getToolStatus() == Tools.STATUS_FREE)
                        .collect(Collectors.toList());
                if (freeFryPans.isEmpty()) {
                    System.out.println("无空闲煎锅，订单" + order.getOrderId() + "放回等待队列");
                    orderWaitQueue.offer(order);
                    rollbackResources(allocatedTools, robot, workspaceAllocated, originalWorkspaceUsed);
                    return false;
                }
                Tools fryPan = freeFryPans.get(0);
                fryPan.setToolStatus(Tools.STATUS_OCCUPIED);
                fryPan.setOccupiedByRobotId(robot.getRobotId());
                robot.setOccupiedFryPan(fryPan);
                allocatedTools.add(fryPan);
                System.out.println("煎锅" + fryPan.getToolId() + "预分配成功");
            }

            // 订单状态变更为“处理中”，并更新到数据库
            order.setOrderStatus(Order.OrderStatus.PROCESSING);
           // Order updatedOrder = orderService.saveOrder(order); // 保存状态更新

            // 机器人关联更新后的订单
            robot.setRobotStatus(Robot.STATUS_BUSY);
            //robot.setCurrentOrder(updatedOrder);
            robot.setCurrentOrder(order);
            //robotRepository.save(robot);

            System.out.println("机器人" + robot.getRobotId() + "分配资源成功，处理订单" + order.getOrderId());
            return true;

        } catch (Exception e) {
            System.err.println("资源分配异常，触发回滚: " + e.getMessage());
            rollbackResources(allocatedTools, robot, workspaceAllocated, originalWorkspaceUsed);
            return false;
        }
    }

    //回滚所有已分配的资源（工具和工作区）
    private void rollbackResources(List<Tools> allocatedTools, Robot robot,
                                   boolean workspaceAllocated, int originalWorkspaceUsed) {
        // 1. 回滚工具资源
        if (!allocatedTools.isEmpty()) {
            for (Tools tool : allocatedTools) {
                tool.setToolStatus(Tools.STATUS_FREE);
                tool.setOccupiedByRobotId(null);
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
        //robotRepository.save(robot);
    }

    private void releaseResource(Robot robot) {
        System.out.println("开始释放机器人" + robot.getRobotId() + "的资源");
        Order order = robot.getCurrentOrder();
        if (order == null) return;
        Dish dish = order.getDish();
        if (robot.getOccupiedOven() != null) {
            Tools oven = robot.getOccupiedOven();
            oven.setToolStatus(Tools.STATUS_FREE);
            oven.setOccupiedByRobotId(null);
            robot.setOccupiedOven(null);
        }
        if (robot.getOccupiedFryPan() != null) {
            Tools fryPan = robot.getOccupiedFryPan();
            fryPan.setToolStatus(Tools.STATUS_FREE);
            fryPan.setOccupiedByRobotId(null);
            robot.setOccupiedFryPan(null);
        }
        int originalUsed = workbench.getUsedSpace();
        workbench.setUsedSpace(Math.max(originalUsed - dish.getRequiredSpace(), 0));
        workbench.setOccupiedByRobotId(null);
        System.out.println("释放工作区空间：原已用" + originalUsed + "→新已用" + workbench.getUsedSpace());

        robot.setOccupiedWorkbench(null);
        // 订单状态变更为“已完成”，设置完成时间并更新到数据库
        order.setOrderStatus(Order.OrderStatus.COMPLETED);
        order.setCompleteTime(LocalDateTime.now());
        //orderService.saveOrder(order); // 保存状态更新

        robot.setCurrentOrder(null);
        robot.setRobotStatus(Robot.STATUS_FREE);
        //robotRepository.save(robot);

        System.out.println("机器人" + robot.getRobotId() + "释放资源，订单" + order.getOrderId() + "完成");
    }

    private void simulateOrderProcessing(Robot robot, Order order) {
        new Thread(() -> {
            try {
                System.out.println("订单开始制作:" + order.getOrderId());
                Thread.sleep(50);
                System.out.println("订单制作时间到:" + order.getOrderId());
                //transactionTemplate.execute(status -> {
                    releaseResource(robot);
                    //return null;
                //});
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
        if (currentTime - lastPrintTime < PRINT_INTERVAL) {
            return;
        }
        lastPrintTime = currentTime;

        System.out.println("\n===== 当前资源状态 =====");
        System.out.println("1. 工具状态：");
        for (Tools tool : allTools) {
            String status = tool.getToolStatus() == Tools.STATUS_FREE ? "空闲" : "被机器人" + tool.getOccupiedByRobotId() + "占用";
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

    // 接收订单list
    public void receiveOrderList(List<Order> orderList) {
        if (orderList == null || orderList.isEmpty()) {
            System.out.println("接收的订单列表为空，跳过处理");
            return;
        }
        // 把列表中的订单逐个加入等待队列
        for (Order order : orderList) {
            if (order != null && order.getDish() != null) {
                try {
                    orderWaitQueue.put(order);
                    System.out.println("接收订单 " + order.getOrderId() + "（菜品：" + order.getDish().getDishName() + "）");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("订单 " + order.getOrderId() + " 加入队列失败");
                }
            }
        }
        System.out.println("已接收订单列表，共 " + orderList.size() + " 个订单");
    }
}