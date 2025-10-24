package com.example.robotdelivery.service;

import com.example.robotdelivery.pojo.MemorySnapshot;
import java.util.Arrays;
import com.example.robotdelivery.pojo.Dish;
import com.example.robotdelivery.pojo.Memory;
import com.example.robotdelivery.pojo.Order;
import com.example.robotdelivery.pojo.Order.OrderStatus;
import com.example.robotdelivery.pojo.Robot;
import com.example.robotdelivery.pojo.Tools;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BankerAlgorithm {
    // 移除：不再需要记录真实Memory的临时数据
    // private int preSimulateUsedSpace;

    /**
     * 检查资源是否安全：使用Memory快照，不修改真实数据
     */
    public boolean isResourceSafe(Robot robot, Order order, List<Tools> allTools, List<Robot> allRobots, Memory realMemory) {
        Dish dish = order.getDish();
        // 1. 创建真实Memory的快照（复制当前真实数据）
        MemorySnapshot snapshot = new MemorySnapshot(realMemory);
        // 2. 创建工具列表的副本（避免模拟分配修改真实工具状态）
        List<Tools> toolSnapshots = copyTools(allTools);
        // 3. 创建机器人副本（避免模拟分配修改真实机器人状态）
        Robot robotSnapshot = copyRobot(robot);

        // 4. 模拟分配：仅操作副本，不影响真实数据
        simulateAllocate(robotSnapshot, order, toolSnapshots, snapshot);

        // 5. 检查安全状态：基于副本数据判断
        boolean isSafe = checkSystemSafe(toolSnapshots, allRobots, snapshot, robotSnapshot,realMemory);

        // 6. 无需撤销模拟分配：因为操作的是副本，直接丢弃即可
        System.out.println("银行家算法判断结果：" + isSafe);
        return isSafe;
    }

    // 复制工具列表：避免模拟分配修改真实工具状态
    private List<Tools> copyTools(List<Tools> realTools) {
        return realTools.stream().map(tool -> {
            Tools copy = new Tools();
            copy.setToolId(tool.getToolId());
            copy.setToolType(tool.getToolType());
            copy.setToolStatus(tool.getToolStatus());
            copy.setOccupiedByRobotId(tool.getOccupiedByRobotId());
            return copy;
        }).collect(Collectors.toList());
    }

    // 复制机器人：避免模拟分配修改真实机器人状态
    private Robot copyRobot(Robot realRobot) {
        Robot copy = new Robot();
        copy.setRobotId(realRobot.getRobotId());
        copy.setRobotStatus(realRobot.getRobotStatus());
        copy.setCurrentOrder(realRobot.getCurrentOrder());
        copy.setOccupiedOven(realRobot.getOccupiedOven() != null ? copyTools(Collections.singletonList(realRobot.getOccupiedOven())).get(0) : null);
        copy.setOccupiedFryPan(realRobot.getOccupiedFryPan() != null ? copyTools(Collections.singletonList(realRobot.getOccupiedFryPan())).get(0) : null);
        // 新增：炸锅副本复制
        copy.setOccupiedFryPot(realRobot.getOccupiedFryPot() != null ? copyTools(Collections.singletonList(realRobot.getOccupiedFryPot())).get(0) : null);

        return copy;
    }

    // 模拟分配：仅操作副本（RobotSnapshot、ToolSnapshots、MemorySnapshot）
    private void simulateAllocate(Robot robotSnapshot, Order order, List<Tools> toolSnapshots, MemorySnapshot snapshot) {
        Dish dish = order.getDish();
        System.out.println("开始给机器人" + robotSnapshot.getRobotId() + "模拟分配资源，需要烤箱：" + dish.getNeedOven());

        // 模拟分配烤箱（操作工具副本）
        if (dish.getNeedOven()) {
            List<Tools> freeOvens = toolSnapshots.stream()
                    .filter(t -> t.getToolType() == Tools.ToolType.OVEN && t.getToolStatus() == Tools.STATUS_FREE)
                    .collect(Collectors.toList());
            System.out.println("可用烤箱数量：" + freeOvens.size());

            if (!freeOvens.isEmpty()) {
                Tools oven = freeOvens.get(0);
                oven.setToolStatus(Tools.STATUS_OCCUPIED);
                oven.setOccupiedByRobotId(robotSnapshot.getRobotId());
                robotSnapshot.setOccupiedOven(oven);
                System.out.println("机器人" + robotSnapshot.getRobotId() + "成功分配烤箱" + oven.getToolId());
            } else {
                System.out.println("机器人" + robotSnapshot.getRobotId() + "没有可用烤箱！");
            }
        }

        // 模拟分配煎锅（操作工具副本）
        if (dish.getNeedFryPan()) {
            List<Tools> freeFryPans = toolSnapshots.stream()
                    .filter(t -> t.getToolType() == Tools.ToolType.FRY_PAN && t.getToolStatus() == Tools.STATUS_FREE)
                    .collect(Collectors.toList());
            System.out.println("可用煎锅数量：" + freeFryPans.size());

            if (!freeFryPans.isEmpty()) {
                Tools fryPan = freeFryPans.get(0);
                fryPan.setToolStatus(Tools.STATUS_OCCUPIED);
                fryPan.setOccupiedByRobotId(robotSnapshot.getRobotId());
                robotSnapshot.setOccupiedFryPan(fryPan);
                System.out.println("机器人" + robotSnapshot.getRobotId() + "成功分配煎锅" + fryPan.getToolId());
            } else {
                System.out.println("机器人" + robotSnapshot.getRobotId() + "没有可用煎锅！");
            }
        }
        // 新增：炸锅模拟分配
        if (dish.getNeedFryPot() != null && dish.getNeedFryPot()) {
            List<Tools> freeFryPots = toolSnapshots.stream()
                    .filter(t -> t.getToolType() == Tools.ToolType.FRY_POT && t.getToolStatus() == Tools.STATUS_FREE)
                    .collect(Collectors.toList());
            System.out.println("可用炸锅数量：" + freeFryPots.size());

            if (!freeFryPots.isEmpty()) {
                Tools fryPot = freeFryPots.get(0);
                fryPot.setToolStatus(Tools.STATUS_OCCUPIED);
                fryPot.setOccupiedByRobotId(robotSnapshot.getRobotId());
                robotSnapshot.setOccupiedFryPot(fryPot); // 需Robot类支持setOccupiedFryPot
                System.out.println("机器人" + robotSnapshot.getRobotId() + "成功分配炸锅" + fryPot.getToolId());
            } else {
                System.out.println("机器人" + robotSnapshot.getRobotId() + "没有可用炸锅！");
            }
        }

        // 模拟分配工作区（操作快照，不影响真实数据）
        int requiredSpace = dish.getRequiredSpace();
        System.out.println("需要工作区空间：" + requiredSpace + "，当前已用（快照）：" + snapshot.getUsedSpace());
        if (snapshot.getUsedSpace() + requiredSpace <= snapshot.getTotalSpace()) {
            snapshot.setUsedSpace(snapshot.getUsedSpace() + requiredSpace);
            snapshot.setOccupiedByRobotId(robotSnapshot.getRobotId());
            robotSnapshot.setOccupiedWorkbench(null); // 快照无需关联真实工作区
            System.out.println("机器人" + robotSnapshot.getRobotId() + "成功分配工作区（快照）");
        } else {
            System.out.println("机器人" + robotSnapshot.getRobotId() + "工作区空间不足（快照）！");
        }

        // 模拟关联订单和设置状态（操作机器人副本）
        robotSnapshot.setCurrentOrder(order);
        robotSnapshot.setRobotStatus(Robot.STATUS_BUSY);
        System.out.println("机器人" + robotSnapshot.getRobotId() + "关联订单：" + order.getOrderId() + "，状态设为忙碌（快照）");
    }

    // 检查系统安全：基于副本数据判断
    private boolean checkSystemSafe(List<Tools> toolSnapshots, List<Robot> realRobots, MemorySnapshot snapshot, Robot simulatedRobot,Memory realMemory) {
        System.out.println("开始检查系统安全性，共" + realRobots.size() + "个机器人（含模拟机器人）");

        // 1. 先检查模拟分配的机器人是否能完成订单（基于快照数据）
        System.out.println("检查模拟机器人" + simulatedRobot.getRobotId() + "，状态：" + simulatedRobot.getRobotStatus());
        boolean simulatedRobotSafe = isRobotSafe(simulatedRobot, snapshot);
        System.out.println("模拟机器人" + simulatedRobot.getRobotId() + "是否能完成订单：" + simulatedRobotSafe);
        if (simulatedRobotSafe) {
            return true;
        }

        // 2. 再检查真实机器人是否能完成订单（基于真实数据）
        for (Robot realRobot : realRobots) {
            if (realRobot.getRobotId() == simulatedRobot.getRobotId()) {
                continue; // 跳过已检查的模拟机器人
            }
            System.out.println("检查真实机器人" + realRobot.getRobotId() + "，状态：" + realRobot.getRobotStatus());
            boolean realRobotSafe = isRobotSafe(realRobot, new MemorySnapshot(realMemory)); // 基于真实数据的快照
            System.out.println("真实机器人" + realRobot.getRobotId() + "是否能完成订单：" + realRobotSafe);
            if (realRobotSafe) {
                return true;
            }
        }

        return false;
    }

    // 判断单个机器人是否能完成订单（兼容真实机器人和模拟机器人）
    private boolean isRobotSafe(Robot robot, MemorySnapshot memorySnapshot) {
        if (robot.getRobotStatus() != Robot.STATUS_BUSY) {
            return false;
        }

        Order order = robot.getCurrentOrder();
        if (order == null || order.getDish() == null) {
            return false;
        }
        Dish dish = order.getDish();

        // 检查工具是否齐全（真实机器人用真实工具，模拟机器人用副本工具）
        boolean hasOven = !dish.getNeedOven() || (robot.getOccupiedOven() != null && robot.getOccupiedOven().getToolStatus() == Tools.STATUS_OCCUPIED);
        boolean hasFryPan = !dish.getNeedFryPan() || (robot.getOccupiedFryPan() != null && robot.getOccupiedFryPan().getToolStatus() == Tools.STATUS_OCCUPIED);
        // 新增：炸锅检查
        boolean hasFryPot = !dish.getNeedFryPot() || (robot.getOccupiedFryPot() != null && robot.getOccupiedFryPot().getToolStatus() == Tools.STATUS_OCCUPIED);

        // 检查工作区是否满足（真实机器人用真实数据快照，模拟机器人用模拟快照）
        boolean hasWorkbench = memorySnapshot.getOccupiedByRobotId() != null && memorySnapshot.getOccupiedByRobotId().equals(robot.getRobotId());

        System.out.println("资源检查结果 - 烤箱：" + hasOven + "，煎锅：" + hasFryPan + "，炸锅：" + hasFryPot + "，工作区：" + hasWorkbench);
        return hasOven && hasFryPan && hasFryPot && hasWorkbench; // 新增炸锅条件
    }
}