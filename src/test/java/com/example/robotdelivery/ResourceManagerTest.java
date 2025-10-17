package com.example.robotdelivery;

import com.example.robotdelivery.pojo.Dish;
import com.example.robotdelivery.pojo.Order;
import com.example.robotdelivery.pojo.Dish.DishType;
import com.example.robotdelivery.service.BankerAlgorithm;
import com.example.robotdelivery.service.ResourceManagerThread;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ResourceManagerTest {
    public static void main(String[] args) throws InterruptedException {
        // 1. 初始化资源管理线程及核心依赖（只初始化必须的 BankerAlgorithm）
        ResourceManagerThread resourceManager = new ResourceManagerThread();
        BankerAlgorithm bankerAlgorithm = new BankerAlgorithm();
        injectDependency(resourceManager, "bankerAlgorithm", bankerAlgorithm);

        // 2. 测试类模拟“订单接收线程”：生成5个随机订单并打包成List
        List<Order> orderList = generateRandomOrderList(5);

        // 3. 把订单列表传给资源管理线程（模拟订单接收线程的传参）
        resourceManager.receiveOrderList(orderList);

        // 4. 启动资源管理线程，开始处理订单
        System.out.println("\n=== 启动资源管理线程，开始处理订单 ===");
        resourceManager.start();

        // 5. 等待线程处理（根据订单数量调整休眠时间，5个订单30秒足够）
        TimeUnit.SECONDS.sleep(30);

        // 6. 停止线程，结束测试
        resourceManager.interrupt();
        System.out.println("=== 测试结束，资源管理线程停止 ===");
    }

    /**
     * 模拟订单接收线程：生成指定数量的随机订单，打包成List
     * @param count 订单数量（这里传5）
     * @return 订单列表
     */
    private static List<Order> generateRandomOrderList(int count) {
        List<Order> orderList = new ArrayList<>();
        Random random = new Random();
        // 准备3种菜品（对应A/B/C类型，模拟不同资源需求）
        List<Dish> dishList = initDishList();

        // 生成count个随机订单
        for (int i = 1; i <= count; i++) {
            Order order = new Order();
            order.setOrderId(i); // 手动设ID（模拟订单生成逻辑）
            order.setCreateTime(LocalDateTime.now());
            order.setOrderStatus(Order.OrderStatus.PENDING); // 初始状态：待处理

            // 随机选一种菜品
            Dish randomDish = dishList.get(random.nextInt(dishList.size()));
            order.setDish(randomDish);
            order.setPriority(random.nextInt(dishList.size()));

            orderList.add(order);
        }
        return orderList;
    }

    /**
     * 初始化3种菜品（A/B/C类型，对应不同资源需求）
     */
    private static List<Dish> initDishList() {
        List<Dish> dishList = new ArrayList<>();

        // 菜品1：A类（需烤箱，占20空间）
        Dish dishA = new Dish();
        dishA.setDishId(1);
        dishA.setDishName("A类菜品（烤鸡）");
        dishA.setDishType(DishType.A);
        dishA.setNeedOven(true);
        dishA.setNeedFryPan(false);
        dishA.setRequiredSpace(20);
        dishList.add(dishA);

        // 菜品2：B类（需煎锅，占40空间）
        Dish dishB = new Dish();
        dishB.setDishId(2);
        dishB.setDishName("B类菜品（煎蛋）");
        dishB.setDishType(DishType.B);
        dishB.setNeedOven(false);
        dishB.setNeedFryPan(true);
        dishB.setRequiredSpace(40);
        dishList.add(dishB);

        // 菜品3：C类（需烤箱+煎锅，占50空间）
        Dish dishC = new Dish();
        dishC.setDishId(3);
        dishC.setDishName("C类菜品（烤煎套餐）");
        dishC.setDishType(DishType.C);
        dishC.setNeedOven(true);
        dishC.setNeedFryPan(true);
        dishC.setRequiredSpace(50);
        dishList.add(dishC);

        return dishList;
    }

    /**
     * 反射工具：给 ResourceManagerThread 注入依赖（只注入必须的 BankerAlgorithm）
     */
    private static void injectDependency(Object obj, String fieldName, Object value) {
        try {
            // 突破私有字段访问限制
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            System.err.println("注入依赖 " + fieldName + " 失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}