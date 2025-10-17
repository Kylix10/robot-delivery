package com.example.robotdelivery;

import com.example.robotdelivery.pojo.Dish;
import com.example.robotdelivery.pojo.Memory;
import com.example.robotdelivery.pojo.Order;
import com.example.robotdelivery.service.MemoryManager;

import java.util.List;

public class MemoryTest {
    // 初始化 Memory 和 MemoryManager（全局复用）
    private final Memory memory;
    private final MemoryManager memoryManager;

    // 构造器：初始化工作台空间
    public MemoryTest() {
        this.memory = new Memory();
        this.memoryManager = new MemoryManager(memory);
        System.out.println("=== 初始化工作台完成 ===");
        memoryManager.printMemoryStatus();
    }

    /**
     * 批量处理订单列表：为每个订单的Dish分配工作台
     * @param orderList 待处理的订单列表
     */
    public void processOrderList(List<Order> orderList) {
        if (orderList == null || orderList.isEmpty()) {
            System.out.println("订单列表为空，无需处理！");
            return;
        }

        System.out.println("\n=== 开始批量处理 " + orderList.size() + " 个订单 ===");
        for (Order order : orderList) {
            Dish dish = order.getDish();
            if (dish == null) {
                System.out.println("订单ID：" + order.getOrderId() + " 未关联菜品，跳过处理");
                continue;
            }

            int dishId = dish.getDishId();
            int dishSpace = dish.getRequiredSpace();
            System.out.println("\n处理订单：" + order.getOrderName() + "（关联菜品ID：" + dishId + "，所需空间：" + dishSpace + "）");

            // 调用 MemoryManager 分配工作台
            boolean allocateResult = memoryManager.allocateForDish(dish);
            if (allocateResult) {
                System.out.println("菜品ID：" + dishId + " 工作台分配成功");
            } else {
                System.out.println("菜品ID：" + dishId + " 工作台分配失败");
            }
        }

        // 处理完成后打印最终状态
        System.out.println("\n=== 批量订单处理完成 ===");
        memoryManager.printMemoryStatus();
    }

    /**
     * 释放单个订单关联菜品的工作台
     * @param order 待释放的订单（通过订单关联的DishID找到分区）
     * @return 释放结果（true：成功，false：未找到对应分区）
     */
    public boolean releaseOrderDish(Order order) {
        if (order == null || order.getDish() == null) {
            System.out.println("订单未关联菜品，无法释放工作台");
            return false;
        }

        int dishId = order.getDish().getDishId();
        System.out.println("\n=== 尝试释放订单：" + order.getOrderName() + "（关联菜品ID：" + dishId + "）的工作台 ===");
        boolean releaseResult = memoryManager.releaseDishPartition(dishId);

        if (releaseResult) {
            System.out.println("菜品ID：" + dishId + " 工作台释放成功");
        } else {
            System.out.println("菜品ID：" + dishId + " 未找到分配记录，释放失败");
        }

        // 释放后打印状态
        memoryManager.printMemoryStatus();
        return releaseResult;
    }

    // 测试入口：模拟创建订单列表并处理
    public static void main(String[] args) {
        // 1. 初始化测试类
        MemoryTest memoryTest = new MemoryTest();

        // 2. 模拟创建订单列表（实际项目中从业务层获取）
        List<Order> orderList = mockOrderList();

        // 3. 批量处理订单的工作台分配
        memoryTest.processOrderList(orderList);

        // 4. 模拟释放第一个订单的工作台（可选操作）
        if (!orderList.isEmpty()) {
            Order orderToRelease = orderList.get(0);
            memoryTest.releaseOrderDish(orderToRelease);
        }
    }

    /**
     * 模拟订单列表数据（实际项目中无需此方法，直接接收业务层传递的List<Order>）
     */
    private static List<Order> mockOrderList() {
        // 模拟菜品1：ID=101，所需空间=20
        Dish dish1 = new Dish();
        dish1.setDishId(101);
        dish1.setDishName("宫保鸡丁");
        dish1.setRequiredSpace(20);

        // 模拟菜品2：ID=102，所需空间=30
        Dish dish2 = new Dish();
        dish2.setDishId(102);
        dish2.setDishName("鱼香肉丝");
        dish2.setRequiredSpace(30);

        // 模拟订单1：关联菜品1
        Order order1 = new Order();
        order1.setOrderId(1);
        order1.setOrderName("用户A的订单");
        order1.setDish(dish1);

        // 模拟订单2：关联菜品2
        Order order2 = new Order();
        order2.setOrderId(2);
        order2.setOrderName("用户B的订单");
        order2.setDish(dish2);

        // 返回订单列表
        return List.of(order1, order2);
    }
}