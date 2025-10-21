package com.example.robotdelivery.service;

import com.example.robotdelivery.mapper.RobotRepository;
import com.example.robotdelivery.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
// 导入 MemoryManager
import com.example.robotdelivery.service.MemoryManager;
import com.example.robotdelivery.vo.OrderScheduleResult;
import com.example.robotdelivery.service.PrioritySchedulingAlgorithm;

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
    //磁盘调度的接口
    private DiskSchedulerInterface diskScheduler;
    @Autowired
    BankerAlgorithm bankerAlgorithm;
    @Autowired
    private PlanningService planningService; // 注入规划服务，用于复用打印逻辑

    // 新增：工作台内存动态分配管理器
    private final MemoryManager memoryManager;

    @Autowired
    RobotRepository robotRepository;

    // 新增：注入OrderService
    @Autowired
    private OrderService orderService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final List<Tools> allTools = initTools();
    private final List<Robot> allRobots = initRobots();
    private final Memory workbench = new Memory();
    private final BlockingQueue<Order> orderWaitQueue = new LinkedBlockingQueue<>();
    private final Object resourceLock = new Object(); // 资源分配锁


    private long lastPrintTime = 0;
    private static final long PRINT_INTERVAL = 500;

    public ResourceManagerThread() {
        // 在构造器中初始化 MemoryManager，传入 Memory 对象
        this.memoryManager = new MemoryManager(workbench);
    }

    // 存数据库用
    // @PostConstruct
    // public void initRobots() {
    // this.allRobots = initAndSaveRobots();
    // }


    private List<Tools> initTools() {
        List<Tools> tools = new ArrayList<>();
        // 烤箱（2个）
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

        // 煎锅（2个，修复ID重复问题）
        Tools fryPan1 = new Tools();
        fryPan1.setToolId(3);
        fryPan1.setToolType(Tools.ToolType.FRY_PAN);
        fryPan1.setToolStatus(Tools.STATUS_FREE);
        tools.add(fryPan1);

        Tools fryPan2 = new Tools();
        fryPan2.setToolId(4); // 修正为唯一ID
        fryPan2.setToolType(Tools.ToolType.FRY_PAN);
        fryPan2.setToolStatus(Tools.STATUS_FREE);
        tools.add(fryPan2);

        // 新增：炸锅（1个，适配新Dish的needFryPot属性）
        Tools fryPot = new Tools();
        fryPot.setToolId(5);
        fryPot.setToolType(Tools.ToolType.FRY_POT); // 需确保Tools类的ToolType枚举包含FRY_POT
        fryPot.setToolStatus(Tools.STATUS_FREE);
        tools.add(fryPot);

        return tools;
    }

    private List<Robot> initRobots() {
        List<Robot> robots = new ArrayList<>();
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
    @Override
    public void run() {
        System.out.println("资源管理线程启动，初始资源：2烤箱+1煎锅+2机器人+工作区" + workbench.getTotalSpace() + "空间");
        memoryManager.printMemoryStatus(); // 打印初始工作台状态

        final long LOOP_DELAY = 1000;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Order order = orderWaitQueue.take();
                if (order == null || order.getDish() == null) {
                    System.out.println("跳过无效订单（订单或菜品为空）");
                    Thread.sleep(LOOP_DELAY);
                    continue;
                }
                 // 定义并赋值dish变量
                Dish dish = order.getDish();
                // 新增：空指针防护（数据库查询的dish可能缺少requiredSpace等属性）
                if (dish == null) {
                    System.out.println("订单" + order.getOrderId() + "的菜品为null，跳过处理");
                    Thread.sleep(LOOP_DELAY);
                    continue;
                }
                // 再判断 requiredSpace
                if (dish.getRequiredSpace() == null) {
                    System.out.println("订单" + order.getOrderId() + "菜品空间未设置，跳过处理");
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

                // 银行家算法安全检查
                boolean isSafe = bankerAlgorithm.isResourceSafe(
                        freeRobot,
                        order,
                        allTools.stream().filter(t -> t.getToolStatus() == Tools.STATUS_FREE).collect(Collectors.toList()),
                        allRobots,
                        workbench
                );
                if (isSafe) {
                    //新加入执行从仓库拿取食材的路径规划 加在银行家算法后面
                    OrderScheduleResult orderScheduleResult = diskScheduler.handleOrderSchedule(order);
                    if (orderScheduleResult == null) {
                        System.out.println("仓库路径规划失败，订单" + order.getOrderId() + "放回等待队列");
                        orderWaitQueue.offer(order);
                        Thread.sleep(LOOP_DELAY);
                        continue;
                    }
                    
                    //执行资源分配
                    boolean allocateSuccess = allocateResource(freeRobot, order);
                    if (allocateSuccess) {
                        simulateOrderProcessing(freeRobot, order);
                    }
                }
                 else {
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


    private boolean allocateResource(Robot robot, Order order) {
        synchronized (resourceLock) { // 加锁确保线程安全
            Dish dish = order.getDish();
            List<Tools> allocatedTools = new ArrayList<>();
            boolean workspaceAllocated = false;

            System.out.println("\n--- 尝试为订单" + order.getOrderId() + "（菜品：" + dish.getDishName() + "，空间：" + dish.getRequiredSpace() + "）分配资源 ---");

            try {
                // 1. 工作台内存动态分配
                if (!memoryManager.allocateForDish(dish)) {
                    System.out.println("工作区空间分配失败（动态分配），订单" + order.getOrderId() + "放回等待队列");
                    orderWaitQueue.offer(order);
                    return false;
                }
                robot.setOccupiedWorkbench(workbench);
                workspaceAllocated = true;
                System.out.println("工作区动态分配成功");
                memoryManager.printMemoryStatus();

                // 2. 烤箱分配
                if (dish.getNeedOven()) {
                    List<Tools> freeOvens = allTools.stream()
                            .filter(t -> t.getToolType() == Tools.ToolType.OVEN && t.getToolStatus() == Tools.STATUS_FREE)
                            .collect(Collectors.toList());
                    if (freeOvens.isEmpty()) {
                        System.out.println("无空闲烤箱，订单" + order.getOrderId() + "放回等待队列");
                        orderWaitQueue.offer(order);
                        rollbackResources(allocatedTools, robot, workspaceAllocated, dish.getDishId());
                        return false;
                    }
                    Tools oven = freeOvens.get(0);
                    oven.setToolStatus(Tools.STATUS_OCCUPIED);
                    oven.setOccupiedByRobotId(robot.getRobotId());
                    robot.setOccupiedOven(oven);
                    allocatedTools.add(oven);
                    System.out.println("烤箱" + oven.getToolId() + "预分配成功");
                }

                // 3. 煎锅分配
                if (dish.getNeedFryPan()) {
                    List<Tools> freeFryPans = allTools.stream()
                            .filter(t -> t.getToolType() == Tools.ToolType.FRY_PAN && t.getToolStatus() == Tools.STATUS_FREE)
                            .collect(Collectors.toList());
                    if (freeFryPans.isEmpty()) {
                        System.out.println("无空闲煎锅，订单" + order.getOrderId() + "放回等待队列");
                        orderWaitQueue.offer(order);
                        rollbackResources(allocatedTools, robot, workspaceAllocated, dish.getDishId());
                        return false;
                    }
                    Tools fryPan = freeFryPans.get(0);
                    fryPan.setToolStatus(Tools.STATUS_OCCUPIED);
                    fryPan.setOccupiedByRobotId(robot.getRobotId());
                    robot.setOccupiedFryPan(fryPan);
                    allocatedTools.add(fryPan);
                    System.out.println("煎锅" + fryPan.getToolId() + "预分配成功");
                }

                // 4. 炸锅分配
                if (dish.getNeedFryPot() != null && dish.getNeedFryPot()) {
                    List<Tools> freeFryPots = allTools.stream()
                            .filter(t -> t.getToolType() == Tools.ToolType.FRY_POT && t.getToolStatus() == Tools.STATUS_FREE)
                            .collect(Collectors.toList());
                    if (freeFryPots.isEmpty()) {
                        System.out.println("无空闲炸锅，订单" + order.getOrderId() + "放回等待队列");
                        orderWaitQueue.offer(order);
                        rollbackResources(allocatedTools, robot, workspaceAllocated, dish.getDishId());
                        return false;
                    }
                    Tools fryPot = freeFryPots.get(0);
                    fryPot.setToolStatus(Tools.STATUS_OCCUPIED);
                    fryPot.setOccupiedByRobotId(robot.getRobotId());
                    robot.setOccupiedFryPot(fryPot);
                    allocatedTools.add(fryPot);
                    System.out.println("炸锅" + fryPot.getToolId() + "预分配成功");
                }

                // ========== 直接调用PlanningService的planForLatestOrders()方法打印结果 ==========
                System.out.println("[资源分配] 调用PlanningService生成路径规划结果：");
                planningService.planForLatestOrders(); // 直接调用，由该方法负责打印
                // ========== 调用结束 ==========


                // 5. 所有资源分配完成，更新数据库（事务内）
                transactionTemplate.execute(status -> {
                    // 更新机器人状态：忙碌 + 绑定当前订单
                    robot.setRobotStatus(Robot.STATUS_BUSY);
                    robot.setCurrentOrder(order);
                    robotRepository.save(robot);

                    // 更新订单状态：处理中
                    order.setOrderStatus(Order.OrderStatus.PROCESSING);
                    orderService.save(order); // 假设orderService已注入且支持save方法

                    return null;
                });

                System.out.println("机器人" + robot.getRobotId() + "分配资源成功，数据库已同步，开始处理订单" + order.getOrderId());
                return true;

            } catch (Exception e) {
                // 任何步骤失败均触发回滚
                System.err.println("资源分配异常，触发回滚: " + e.getMessage());
                rollbackResources(allocatedTools, robot, workspaceAllocated, dish.getDishId());
                return false;
            }
        }
    }
    /**
     * 回滚所有已分配的资源（工具和工作区）
     * @param allocatedTools 已分配的工具列表
     * @param robot 机器人对象
     * @param workspaceAllocated 工作区是否已分配标志
     * @param dishId 菜肴ID (用于动态内存管理释放分区)
     */
    private void rollbackResources(List<Tools> allocatedTools, Robot robot,
                                   boolean workspaceAllocated, int dishId) {
        System.out.println("--- 执行资源回滚 ---");
        // 1. 回滚工具资源
        // 1. 工具回滚（新增炸锅回滚）
        if (!allocatedTools.isEmpty()) {
            for (Tools tool : allocatedTools) {
                tool.setToolStatus(Tools.STATUS_FREE);
                tool.setOccupiedByRobotId(null);
                if (tool.getToolType() == Tools.ToolType.OVEN) {
                    robot.setOccupiedOven(null);
                } else if (tool.getToolType() == Tools.ToolType.FRY_PAN) {
                    robot.setOccupiedFryPan(null);
                } else if (tool.getToolType() == Tools.ToolType.FRY_POT) { // 新增炸锅回滚
                    robot.setOccupiedFryPot(null);
                }
                System.out.println("已回滚工具: " + tool.getToolType() + "(ID:" + tool.getToolId() + ")");
            }
        }

        // 2. 回滚工作区资源 (修改为调用 MemoryManager 释放)
        if (workspaceAllocated) {
            if (memoryManager.releaseDishPartition(dishId)) {
                System.out.println("已回滚工作区内存（菜肴ID:" + dishId + "）");
            } else {
                System.err.println("!!! 警告：工作区内存回滚失败（菜肴ID:" + dishId + "）!!! ");
            }
            robot.setOccupiedWorkbench(null); // 机器人解绑
        }

        // 3. 重置机器人状态
        robot.setRobotStatus(Robot.STATUS_FREE);
        robot.setCurrentOrder(null);
        // robotRepository.save(robot);
    }

    private void releaseResource(Robot robot) {
        synchronized (resourceLock) { // 加锁
            System.out.println("开始释放机器人" + robot.getRobotId() + "的资源");
            Order order = robot.getCurrentOrder();
            if (order == null || order.getDish() == null) return;
            Dish dish = order.getDish();

            // --- 工具资源释放 ---
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

            // 3. 新增：炸锅释放
            if (robot.getOccupiedFryPot() != null) {
                Tools fryPot = robot.getOccupiedFryPot();
                fryPot.setToolStatus(Tools.STATUS_FREE);
                fryPot.setOccupiedByRobotId(null);
                robot.setOccupiedFryPot(null);
                System.out.println("炸锅" + fryPot.getToolId() + "释放成功");
            }
            // --- 工具资源释放结束 ---

            // --- 核心修改：工作台内存动态释放 ---
            if (memoryManager.releaseDishPartition(dish.getDishId())) {
                System.out.println("工作区内存（菜肴ID:" + dish.getDishId() + "）释放成功");
            } else {
                System.err.println("!!! 警告：工作区内存释放失败（菜肴ID:" + dish.getDishId() + "）!!! ");
            }
            robot.setOccupiedWorkbench(null);
            // --- 核心修改结束 ---

            // 执行数据库更新（事务内）
            transactionTemplate.execute(status -> {
                // 更新订单状态到数据库（标记为已完成）
                order.setOrderStatus(Order.OrderStatus.COMPLETED);
                order.setCompleteTime(LocalDateTime.now());
                orderService.save(order);

                // 更新机器人状态到数据库（标记为空闲）
                robot.setRobotStatus(Robot.STATUS_FREE);
                robot.setCurrentOrder(null);
                robotRepository.save(robot);

                return null;
            });


            System.out.println("机器人" + robot.getRobotId() + "释放资源，订单" + order.getOrderId() + "完成");
        }
    }

    private void simulateOrderProcessing(Robot robot, Order order) {
        new Thread(() -> {
            try {
                System.out.println("订单开始制作:" + order.getOrderId());
                Thread.sleep(500); // 增加模拟时间以便观察
                System.out.println("订单制作时间到:" + order.getOrderId());
                // transactionTemplate.execute(status -> {
                releaseResource(robot);
                // return null;
                // });
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

        // 补充工作台状态打印（原代码遗漏，方便测试查看）
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

        // 在队列添加完毕后，进行优先级调度

        PrioritySchedulingAlgorithm scheduler = new PrioritySchedulingAlgorithm(orderWaitQueue);
        scheduler.sortQueue(); // 调度
        scheduler.printQueue(); // 打印队列
    }

    // 新增 printQueue 方法，用于打印订单等待队列的信息
    public void printQueue() {
        System.out.println("===== 订单等待队列状态 =====");
        if (orderWaitQueue.isEmpty()) {
            System.out.println("订单等待队列为空，没有待处理的订单");
        } else {
            System.out.println("订单等待队列中有 " + orderWaitQueue.size() + " 个待处理订单：");
            for (Order order : orderWaitQueue) {
                System.out.println("订单ID: " + order.getOrderId() + "，菜品: " + order.getDish().getDishName() + "，优先级: " + order.getPriority());
            }
        }
        System.out.println("==========================");
    }

}
/*      留一个线程池的方案，万一机器人数量啥的后面要提升的话，可以考虑用线程池来管理
   初始化线程池，线程数可设为机器人数量
   private final ExecutorService orderExecutor = Executors.newFixedThreadPool(2); // 2 台机器人
    提交订单处理任务到线程池
   private void submitOrderToExecutor(Robot robot, Order order)
   {
     orderExecutor.submit(() ->
     {
         try
         {
             simulateOrderProcessing(robot, order); // 执行订单处理
         }
         catch(Exception e)
         {
             // 异常时释放资源，避免机器人或工具卡住
             release(robot);
             System.err.println("订单处理异常，已释放资源: " + e.getMessage());
             e.printStackTrace();
         }
     });
    }
    //在 ResourceManagerThread 停止时关闭线程池
 private void shutdownExecutor()
     {
     orderExecutor.shutdown(); // 禁止新任务提交
     try
     {
         if (!orderExecutor.awaitTermination(5, TimeUnit.SECONDS))
         {
             orderExecutor.shutdownNow(); // 强制关闭
         }
     }
     catch (InterruptedException e)
     {
         orderExecutor.shutdownNow(); // 中断时强制关闭
         Thread.currentThread().interrupt(); // 保留中断状态
     }
 }
 */
