package com.example.robotdelivery.service;

import com.example.robotdelivery.mapper.DishMapper;
import com.example.robotdelivery.mapper.OrderMapper;
import com.example.robotdelivery.pojo.Dish;
import com.example.robotdelivery.pojo.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service; // 新增：注册为Spring Bean
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class OrderGenerate {
    // 新增：注入资源管理线程
    @Autowired
    private ResourceManagerThread resourceManager; // 用于提交订单给资源管理器
    private final DishMapper dishMapper;
    private final Random random = new Random();
    private final OrderMapper orderMapper;

    // 构造器注入（Spring会自动注入OrderMapper和DishMapper）
    @Autowired // 新增：构造器注入需加@Autowired（或用 Lombok 的 @AllArgsConstructor）
    public OrderGenerate(OrderMapper orderMapper, DishMapper dishMapper) {
        this.orderMapper = orderMapper;
        this.dishMapper = dishMapper;
    }

    /**
     * 定时任务：每10秒生成10个随机订单
     */
    //@Scheduled(fixedRate = 100000)
    public void generateRandomOrders() {
        List<Dish> allDishes = dishMapper.findAll();
        //新增 用来加载dish的所有字段
        for (Dish dish : allDishes) {
            // 显式访问所有需要的字段，触发Hibernate加载
            dish.getDishName();
            dish.getDish_price();
            dish.getNeedOven();
            dish.getNeedFryPan();
            dish.getNeedFryPot();
            dish.getRequiredSpace();
            dish.getCookTime();
        }
        if (allDishes.isEmpty()) {
            System.out.println("[定时] 当前没有可用菜品，无法生成订单。");
            return;
        }

         // 打印验证：确保dish_price有值
                for (Dish dish : allDishes) {
                    System.out.println("菜品：" + dish.getDishName() + "，价格：" + dish.getDish_price() + "元");
                }


        List<Order> createdOrders = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Order order = new Order();
            order.setOrderName("Order-" + System.currentTimeMillis() + "-" + i);

            // 随机选择菜品
            Dish randomDish = allDishes.get(random.nextInt(allDishes.size()));
            order.setDish(randomDish);

            // 随机优先级 1~5
            order.setPriority(random.nextInt(5) + 1);

            // 设置创建时间
            order.setCreateTime(LocalDateTime.now());

            // 初始状态为 PENDING
            order.setOrderStatus(Order.OrderStatus.PENDING);

            // 保存订单
            orderMapper.save(order);
            createdOrders.add(order);
        }

        System.out.println("[定时] 成功生成 10 个随机订单：");
        //createdOrders.forEach(o -> System.out.println(formatOrder(o)));

        // 核心修改：调用资源管理器的receiveOrderList方法提交订单列表
        resourceManager.receiveOrderList(createdOrders);
    }

}
