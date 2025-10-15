package com.example.robotdelivery.service;

import com.example.robotdelivery.pojo.Dish;
import com.example.robotdelivery.pojo.Memory;
import com.example.robotdelivery.pojo.Order;
import com.example.robotdelivery.pojo.Order.OrderStatus;
import com.example.robotdelivery.pojo.Robot;
import com.example.robotdelivery.pojo.Tools;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BankerAlgorithm {
    // 单线程专用：记录当前模拟分配前的工作区已用空间
    private int preSimulateUsedSpace;

    /**
     * 检查机器人申请资源是否安全（分配后系统无死锁）
     * @param robot 申请资源的机器人
     * @param order 机器人当前订单（已关联真实dish）
     * @param allTools 所有工具资源
     * @param allRobots 新增参数：所有真实机器人列表（从ResourceManagerThread传入）
     * @param memory 工作区资源
     * @return true=安全可分配，false=不安全需等待
     */
    public boolean isResourceSafe(Robot robot, Order order, List<Tools> allTools, List<Robot> allRobots, Memory memory) {
        Dish dish = order.getDish();
        // 1. 模拟分配：假设将资源分配给该机器人（仅修改内存状态，不影响真实数据）
        simulateAllocate(robot, order, allTools, memory);

        // 2. 检查安全状态：传入真实机器人列表，不再模拟空订单
        boolean isSafe = checkSystemSafe(allTools, allRobots, memory);

        // 3. 撤销模拟分配：恢复到分配前的状态（仅检查，不实际分配）
        undoSimulateAllocate(robot, order, allTools, memory);

        System.out.println(isSafe);
        return isSafe;
    }

    // 模拟分配资源（仅修改内存状态）
    private void simulateAllocate(Robot robot, Order order, List<Tools> allTools, Memory memory) {
        Dish dish = order.getDish();
        // 关键：模拟分配前，先记录当前工作区的已用空间
        preSimulateUsedSpace = memory.getUsedSpace();

        System.out.println("开始给机器人" + robot.getRobotId() + "模拟分配资源，需要烤箱：" + dish.getNeedOven());

        // 模拟分配烤箱
        if (dish.getNeedOven()) {
            List<Tools> freeOvens = allTools.stream()
                    .filter(t -> t.getToolType() == Tools.ToolType.OVEN && t.getStatus() == Tools.STATUS_FREE)
                    .collect(Collectors.toList());
            System.out.println("可用烤箱数量：" + freeOvens.size()); // 新增日志：看是否有空闲烤箱

            if (!freeOvens.isEmpty()) {
                Tools oven = freeOvens.get(0);
                oven.setStatus(Tools.STATUS_OCCUPIED);
                oven.setOccupiedByRobotId(robot.getRobotId());
                robot.setOccupiedOven(oven); // 必须执行这行，否则机器人没记录烤箱
                System.out.println("机器人" + robot.getRobotId() + "成功分配烤箱" + oven.getToolId());
            } else {
                System.out.println("机器人" + robot.getRobotId() + "没有可用烤箱！");
            }
        }

        // 【煎锅分配】（新增代码）
        if (dish.getNeedFryPan()) {
            List<Tools> freeFryPans = allTools.stream()
                    .filter(t -> t.getToolType() == Tools.ToolType.FRY_PAN && t.getStatus() == Tools.STATUS_FREE)
                    .collect(Collectors.toList());
            System.out.println("可用煎锅数量：" + freeFryPans.size());
            if (!freeFryPans.isEmpty()) {
                Tools fryPan = freeFryPans.get(0);
                fryPan.setStatus(Tools.STATUS_OCCUPIED);
                fryPan.setOccupiedByRobotId(robot.getRobotId());
                robot.setOccupiedFryPan(fryPan); // 注意：要给机器人设置占用的煎锅
                System.out.println("机器人" + robot.getRobotId() + "成功分配煎锅" + fryPan.getToolId());
            } else {
                System.out.println("机器人" + robot.getRobotId() + "没有可用煎锅！");
            }
        }


        // 模拟分配工作区（同理加日志）
        int requiredSpace = dish.getRequiredSpace();
        System.out.println("需要工作区空间：" + requiredSpace + "，当前已用：" + memory.getUsedSpace());
        if (memory.getUsedSpace() + requiredSpace <= memory.getTotalSpace()) {
            memory.setUsedSpace(memory.getUsedSpace() + requiredSpace);

            memory.setOccupiedByRobotId(robot.getRobotId());
            robot.setOccupiedWorkbench(memory); // 必须执行这行，否则机器人没记录工作区
            System.out.println("机器人" + robot.getRobotId() + "成功分配工作区");
        } else {
            System.out.println("机器人" + robot.getRobotId() + "工作区空间不足！");
        }

        // 3. 关键：给机器人关联当前订单（必须添加！）
        robot.setCurrentOrder(order);
        System.out.println("机器人" + robot.getRobotId() + "关联订单：" + order.getOrderId());


        // 最后必须标记机器人为忙碌状态
        robot.setRobotStatus(Robot.STATUS_BUSY);
        System.out.println("机器人" + robot.getRobotId() + "状态设置为忙碌：" + robot.getRobotStatus());
    }

    // 撤销模拟分配
    private void undoSimulateAllocate(Robot robot, Order order, List<Tools> allTools, Memory memory) {

        Dish dish = order.getDish();
        // 撤销工具占用
        if (robot.getOccupiedOven() != null) {
            Tools oven = robot.getOccupiedOven();
            oven.setStatus(Tools.STATUS_FREE);
            oven.setOccupiedByRobotId(null);
            robot.setOccupiedOven(null);
        }


        // 撤销煎锅占用（新增代码）
        if (robot.getOccupiedFryPan() != null) {
            Tools fryPan = robot.getOccupiedFryPan();
            fryPan.setStatus(Tools.STATUS_FREE);
            fryPan.setOccupiedByRobotId(null);
            robot.setOccupiedFryPan(null);


        }

        // 撤销工作区占用
        // 直接恢复到模拟分配前的已用空间，不会出现负数
        memory.setUsedSpace(preSimulateUsedSpace);
        memory.setOccupiedByRobotId(null);
        robot.setOccupiedWorkbench(null);
        // 撤销订单/机器人状态
        order.setOrderStatus(OrderStatus.PENDING);
        robot.setRobotStatus(Robot.STATUS_FREE);
    }

    // 检查系统是否安全（是否存在一个订单能完成）
    private boolean isSafe(Robot robot) {

        // 若机器人已占用所有所需资源，则能完成订单（释放资源）
        if (robot.getRobotStatus() == Robot.STATUS_BUSY) {
            Order order = robot.getCurrentOrder();
            // 关键1：必须先判断order和dish非空，否则会空指针
            if (order == null || order.getDish() == null) {

                return false; // 订单或菜品为空，肯定完成不了
            }
            Dish dish = order.getDish();
            // 检查工具是否齐全
            boolean hasOven = !dish.getNeedOven() || robot.getOccupiedOven() != null;
            boolean hasFryPan = !dish.getNeedFryPan() || robot.getOccupiedFryPan() != null;
            // 检查工作区是否占用
            boolean hasWorkbench = robot.getOccupiedWorkbench() != null;

            // 打印三个变量的值（添加这句代码）
            System.out.println("资源检查结果 - 烤箱：" + hasOven + "，煎锅：" + hasFryPan + "，工作区：" + hasWorkbench);

            return hasOven && hasFryPan && hasWorkbench;
        }
        return false;
    }

    /**
     * 检查系统是否安全：遍历真实机器人，判断是否存在能完成订单的机器人
     * @param allTools 所有工具资源（暂无需使用，保留参数兼容）
     * @param allRobots 所有真实机器人（核心：从中获取已关联订单的机器人）
     * @param memory 工作区资源（暂无需使用，保留参数兼容）
     * @return true=系统安全，false=系统不安全
     */
    private boolean checkSystemSafe(List<Tools> allTools, List<Robot> allRobots, Memory memory) {
        System.out.println("开始检查系统安全性，共" + allRobots.size() + "个机器人"); // 确认机器人数量（应为2个）

        for (Robot robot : allRobots) {
            // 打印当前检查的机器人ID和状态，确认是否遍历到了所有机器人
            System.out.println("检查机器人" + robot.getRobotId() + "，状态：" + robot.getRobotStatus());

            // 只有“忙碌中”的机器人才可能在处理订单（模拟分配后，当前机器人应该是BUSY状态）
            if (robot.getRobotStatus() == Robot.STATUS_BUSY) {
                boolean robotSafe = isSafe(robot); // 调用isSafe检查该机器人
                System.out.println("机器人" + robot.getRobotId() + "是否能完成订单：" + robotSafe);
                if (robotSafe) {
                    return true; // 找到一个能完成订单的机器人，系统安全
                }
            }
        }
        return false; // 所有机器人都不能完成订单，系统不安全
    }
}