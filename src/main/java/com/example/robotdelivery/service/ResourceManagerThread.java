package com.example.robotdelivery.service;

import com.example.robotdelivery.mapper.RobotRepository;
import com.example.robotdelivery.pojo.*;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
// 导入 MemoryManager
import com.example.robotdelivery.service.MemoryManager;
import com.example.robotdelivery.vo.OrderScheduleResult;
import com.example.robotdelivery.service.PrioritySchedulingAlgorithm;
// 导入乐观锁冲突异常类
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import javax.annotation.PostConstruct;
import java.util.*;

import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
public class ResourceManagerThread extends Thread {

    @Autowired
    private EntityManager entityManager; // 注入实体管理器

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
    // 新增：注入 ToolManager
    @Autowired
    private ToolManager toolManager;


    private List<Tools> allTools;
   private List<Robot> allRobots; // = initRobots();
    private final Memory workbench = new Memory();
    private final BlockingQueue<Order> orderWaitQueue = new LinkedBlockingQueue<>();
    private final Object resourceLock = new Object(); // 资源分配锁


    private long lastPrintTime = 0;
    private static final long PRINT_INTERVAL = 500;


    // 定义一个全局锁对象（用机器人ID作为锁的key）
    private final ConcurrentHashMap<Integer, Object> robotLocks = new ConcurrentHashMap<>();

    // 获取机器人专属锁（不存在则创建）
    private Object getRobotLock(Integer robotId) {
        return robotLocks.computeIfAbsent(robotId, k -> new Object());
    }



    public ResourceManagerThread() {
        // 在构造器中初始化 MemoryManager，传入 Memory 对象
        this.memoryManager = new MemoryManager(workbench);
    }

    // 存数据库用
    // @PostConstruct
    // public void initRobots() {
    // this.allRobots = initAndSaveRobots();
    // }
    // 从 ToolManager 获取已初始化并集中管理的工具列表

    // 新增：Spring 注入完成后执行初始化
    @PostConstruct
    public void setupTools() {
        this.allTools = toolManager.getAllToolInstances();
        // 确保 Thread 有名字，方便日志区分
        this.setName("Robot-delivery-Resource-Manager");
    }

    public void initRobotsAndSave() {
        // 此时 robotRepository 已被注入，非 null
        List<Robot> existingRobots = robotRepository.findAll();
        if (existingRobots.isEmpty()) {
            // 初始化机器人
            Robot robot1 = new Robot();
            robot1.setRobotStatus(Robot.STATUS_FREE);
            robot1.setFinishedOrders(0);

            Robot robot2 = new Robot();
            robot2.setRobotStatus(Robot.STATUS_FREE);
            robot2.setFinishedOrders(0);

            // 保存到数据库
            allRobots = robotRepository.saveAll(Arrays.asList(robot1, robot2));
            System.out.println("初始化机器人并保存到数据库");
        } else {
            // 从数据库加载已有机器人
            allRobots = existingRobots;
            System.out.println("从数据库加载机器人：" + allRobots.size() + "个");
        }
    }


    private List<Robot> initRobots() {
        List<Robot> robots = new ArrayList<>();

        // 机器人1
        Robot robot1 = new Robot();
        robot1.setRobotId(1);
        robot1.setRobotStatus(Robot.STATUS_FREE);
        robot1.setFinishedOrders(0); // 初始化完成订单数
        robots.add(robot1);

        // 机器人2
        Robot robot2 = new Robot();
        robot2.setRobotId(2);
        robot2.setRobotStatus(Robot.STATUS_FREE);
        robot2.setFinishedOrders(0); // 初始化完成订单数
        robots.add(robot2);

        // 若需要初始化到数据库，取消注释
        robotRepository.saveAll(robots);
        return robots;
    }
    @Override
    public void run() {

        // 新增：确保 allRobots 初始化
        if (allRobots == null || allRobots.isEmpty()) {
            initRobotsAndSave(); // 手动调用初始化方法
            // 重试机制（最多等3秒）
            int retry = 0;
            while ((allRobots == null || allRobots.isEmpty()) && retry < 30) {
                try {
                    Thread.sleep(100);
                    initRobotsAndSave();
                    retry++;
                    System.out.println("重试初始化 allRobots（第" + retry + "次）");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            if (allRobots == null || allRobots.isEmpty()) {
                System.err.println("致命错误：allRobots 初始化失败，线程终止");
                return;
            }
        }
        // 双重保障：确保 allTools 已初始化
        // 1. 若未初始化，手动调用 ToolManager 获取工具列表
        if (allTools == null || allTools.isEmpty()) {
            this.allTools = toolManager.getAllToolInstances();
            // 2. 若获取后仍为空，等待 3 秒重试（给 ToolManager 初始化时间）
            int retry = 0;
            while ((allTools == null || allTools.isEmpty()) && retry < 30) {
                try {
                    Thread.sleep(100);
                    this.allTools = toolManager.getAllToolInstances();
                    retry++;
                    System.out.println("重试获取 allTools（第" + retry + "次）");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            // 3. 重试后仍失败，直接终止线程
            if (allTools == null || allTools.isEmpty()) {
                System.err.println("致命错误：ToolManager 未返回工具列表，资源管理线程终止");
                return;
            }
        }


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
        // 使用resourceLock保证内存资源分配的原子性（工具、工作台等）
        synchronized (resourceLock) {
            //新增：机器人专属锁（关键！同一机器人的分配/释放操作互斥）
            // 锁对象：通过机器人ID获取专属锁
            synchronized (getRobotLock(robot.getRobotId())) {
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

                // 5. 调用路径规划服务
               // System.out.println("[资源分配] 调用PlanningService生成路径规划结果：");
               // planningService.planForLatestOrders();

                // 3. 修改 allocateResource 中的事务逻辑：移除内存状态修改
                transactionTemplate.execute(status -> {
                    Robot latestRobot = robotRepository.findById(robot.getRobotId())
                            .orElseThrow(() -> new RuntimeException("机器人不存在"));
                    // 只修改数据库中的机器人，不修改内存中的 robot 对象
                    latestRobot.setRobotStatus(Robot.STATUS_BUSY);
                    latestRobot.setCurrentOrder(order);
                    robotRepository.save(latestRobot);

                    // 打印机器人关联的订单号
                    if (latestRobot.getCurrentOrder() != null) {
                        System.out.println("机器人" + latestRobot.getRobotId() + "DOITDOITDOIT已关联订单：" + latestRobot.getCurrentOrder().getOrderId());
                    } else {
                        System.out.println("机器人" + latestRobot.getRobotId() + "DOITDOITDOIT未关联订单（异常）");
                    }


                    // 同步内存对象（关键：内存中也绑定订单）
                    robot.setCurrentOrder(order);
                    syncRobotToMemory(latestRobot);
                    return null;
                });

                System.out.println("机器人" + robot.getRobotId() + "分配资源成功，数据库已同步，开始处理订单" + order.getOrderId());
                return true;

            } catch (ObjectOptimisticLockingFailureException e) {
                // 捕获乐观锁冲突异常（版本号不匹配）
                System.err.println("资源分配冲突：机器人" + robot.getRobotId() + "被其他事务修改，触发回滚");
                rollbackResources(allocatedTools, robot, workspaceAllocated, dish.getDishId());
                // 将订单放回队列，等待重试
                orderWaitQueue.offer(order);
                return false;
            } catch (Exception e) {
                // 其他异常（如机器人已被占用、数据库错误等）
                System.err.println("资源分配异常，触发回滚: " + e.getMessage());
                rollbackResources(allocatedTools, robot, workspaceAllocated, dish.getDishId());
                // 非冲突异常，可根据需要决定是否放回队列
                orderWaitQueue.offer(order);
                return false;
            }
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
        System.out.println("=== 开始执行 releaseResource，机器人ID：" + robot.getRobotId() + " ===");
        synchronized (resourceLock) {
            synchronized (getRobotLock(robot.getRobotId())) {
                System.out.println("开始释放机器人" + robot.getRobotId() + "的资源");
                Order order = robot.getCurrentOrder();
                Dish dish = null;
                Integer robotId = robot.getRobotId(); // 提前获取机器人ID，避免后续空指针

                // 步骤1：确保订单和菜品非空（原有逻辑不变）
                if (order == null || order.getDish() == null) {
                    Robot dbRobot = robotRepository.findById(robotId)
                            .orElseThrow(() -> new RuntimeException("机器人不存在"));
                    order = dbRobot.getCurrentOrder();
                    dish = order != null ? order.getDish() : null;
                    robot.setCurrentOrder(order);
                    robot.setRobotStatus(dbRobot.getRobotStatus());
                    System.out.println("警告：内存订单为空，已从数据库刷新状态");
                } else {
                    dish = order.getDish();
                }

                // 步骤2：工具和工作区释放（原有逻辑不变）
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
                if (robot.getOccupiedFryPot() != null) {
                    Tools fryPot = robot.getOccupiedFryPot();
                    fryPot.setToolStatus(Tools.STATUS_FREE);
                    fryPot.setOccupiedByRobotId(null);
                    robot.setOccupiedFryPot(null);
                    System.out.println("炸锅" + fryPot.getToolId() + "释放成功");
                }
                // --- 工作区释放 ---
                if (memoryManager.releaseDishPartition(dish.getDishId())) {
                    System.out.println("工作区内存（菜肴ID:" + dish.getDishId() + "）释放成功");
                } else {
                    System.err.println("!!! 警告：工作区内存释放失败（菜肴ID:" + dish.getDishId() + "）!!! ");
                }
                robot.setOccupiedWorkbench(null);

                // 步骤3：事务逻辑修改（核心！只保留 finalOrder，删除 finalRobot/finalDish）
                final Order finalOrder = order;

                transactionTemplate.execute(status -> {
                    try {
                        System.out.println("开始更新订单" + finalOrder.getOrderId() + "和机器人" + robotId);

                        // 关键修改1：事务内查询最新的机器人对象（获取最新version）
                        Robot latestRobot = robotRepository.findById(robotId)
                                .orElseThrow(() -> new RuntimeException("机器人" + robotId + "不存在"));

                        // 关键修改2：更新订单（原有逻辑不变）
                        finalOrder.setOrderStatus(Order.OrderStatus.COMPLETED);
                        finalOrder.setCompleteTime(LocalDateTime.now());
                        orderService.save(finalOrder);
                        System.out.println("订单" + finalOrder.getOrderId() + "已保存");

                        // 验证订单数据库状态（原有逻辑不变）
                        Optional<Order> orderOptional = orderService.findById(finalOrder.getOrderId());
                        if (orderOptional.isPresent()) {
                            Order dbOrder = orderOptional.get();
                            System.out.println("数据库中订单" + dbOrder.getOrderId() + "真实状态：" + dbOrder.getOrderStatus());
                            System.out.println("数据库中完成时间：" + (dbOrder.getCompleteTime() != null ? dbOrder.getCompleteTime() : "未设置"));
                        } else {
                            System.err.println("数据库中未找到订单" + finalOrder.getOrderId());
                        }

                        // 关键修改3：用最新机器人对象更新状态（version匹配，无冲突）
                        latestRobot.setRobotStatus(Robot.STATUS_FREE);
                        latestRobot.setCurrentOrder(null);
                        robotRepository.save(latestRobot); // 保存最新对象，version正确
                        System.out.println("机器人" + latestRobot.getRobotId() + "已保存（最新version=" + latestRobot.getVersion() + "）");

                        // 关键修改4：同步最新状态到内存
                        syncRobotToMemory(latestRobot);
                        robot.setRobotStatus(Robot.STATUS_FREE); // 同步内存中传入的robot对象
                        robot.setCurrentOrder(null);

                        if (status.isRollbackOnly()) {
                            System.err.println("警告：事务被标记为回滚！");
                        } else {
                            System.out.println("事务正常提交");
                        }
                    } catch (Exception e) {
                        status.setRollbackOnly();
                        System.err.println("事务回滚原因：" + e.getMessage());
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                    return null;
                });

                System.out.println("机器人" + robotId + "释放资源，订单" + finalOrder.getOrderId() + "完成");
            }
        }
    }

    private void simulateOrderProcessing(Robot robot, Order order) {
        new Thread(() -> {
            try {
                System.out.println("订单" + order.getOrderId() + "开始制作，占用机器人" + robot.getRobotId());
                Thread.sleep(100); // 延长模拟时间，确保释放逻辑执行
                System.out.println("订单" + order.getOrderId() + "制作完成，开始释放机器人" + robot.getRobotId());
                releaseResource(robot); // 强制释放
                System.out.println("机器人" + robot.getRobotId() + "释放完成，状态已更新为空闲");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // 中断时强制释放机器人
                releaseResource(robot);
                System.err.println("订单处理中断，已强制释放机器人" + robot.getRobotId());
            }
        }, "Order-Processing-" + order.getOrderId()).start();
    }

    // 1. 修改 findFreeRobot()：从数据库实时查询空闲机器人
    private Robot findFreeRobot() {
        // 不再从内存 allRobots 筛选，直接查数据库最新状态
        List<Robot> freeRobots = robotRepository.findByRobotStatus(Robot.STATUS_FREE);
        if (freeRobots.isEmpty()) {
            System.out.println("无空闲机器人（数据库实时查询）");
            return null;
        }
        // 取第一个空闲机器人，并同步到内存 allRobots
        Robot freeRobot = freeRobots.get(0);
        syncRobotToMemory(freeRobot); // 同步数据库状态到内存
        return freeRobot;
    }

    // 2. 新增：同步数据库机器人状态到内存
    private void syncRobotToMemory(Robot dbRobot) {
        for (int i = 0; i < allRobots.size(); i++) {
            if (allRobots.get(i).getRobotId().equals(dbRobot.getRobotId())) {
                allRobots.set(i, dbRobot); // 用数据库最新状态覆盖内存
                break;
            }
        }
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


        //新增， 将排序的队列彻底复制一份，用于对比测试
        List<Order> copiedOrders = deepCopyOrdersForSimulation(new ArrayList<>(orderWaitQueue));
        DeadlockSimulation simulation = new DeadlockSimulation(copiedOrders);

        new Thread(simulation::runSimulation, "Deadlock-Simulation-Thread").start();




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


    //一个用于彻底复制队列的方法
    private List<Order> deepCopyOrdersForSimulation(List<Order> originalOrders) {
        return DeadlockSimulation.deepCopyOrders(originalOrders);
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
