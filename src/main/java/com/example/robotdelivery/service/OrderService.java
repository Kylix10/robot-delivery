package com.example.robotdelivery.service;

import com.example.robotdelivery.mapper.DishMapper;
import com.example.robotdelivery.mapper.OrderMapper;
import com.example.robotdelivery.pojo.Dish;
import com.example.robotdelivery.pojo.Order;
import com.example.robotdelivery.pojo.Order.OrderStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final DishMapper dishMapper;
    private final Random random = new Random();

    public OrderService(OrderMapper orderMapper, DishMapper dishMapper) {
        this.orderMapper = orderMapper;
        this.dishMapper = dishMapper;
    }

    /**
     * 定时任务：每10秒生成10个随机订单
     */
    @Scheduled(fixedRate = 10000)
    public void generateRandomOrders() {
        List<Dish> allDishes = dishMapper.findAll();
        if (allDishes.isEmpty()) {
            System.out.println("[定时] 当前没有可用菜品，无法生成订单。");
            return;
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
            order.setOrderStatus(OrderStatus.PENDING);

            // 保存订单
            orderMapper.save(order);
            createdOrders.add(order);
        }

        System.out.println("[定时] 成功生成 10 个随机订单：");
        createdOrders.forEach(o -> System.out.println(formatOrder(o)));
    }

    private String formatOrder(Order o) {
        return "Order{" +
                "id=" + o.getOrderId() +
                ", name=" + o.getOrderName() +
                ", priority=" + o.getPriority() +
                ", dish=" + (o.getDish() != null ? o.getDish().getDishName() : null) +
                ", createTime=" + o.getCreateTime() +
                ", completeTime=" + o.getCompleteTime() +
                ", status=" + o.getOrderStatus() +
                '}';
    }

    // 保存/更新订单
    public Order save(Order order) {
        // 设置创建时间和默认状态，如果新订单未赋值
        if (order.getCreateTime() == null) {
            order.setCreateTime(LocalDateTime.now());
        }
        if (order.getOrderStatus() == null) {
            order.setOrderStatus(OrderStatus.PENDING);
        }
        return orderMapper.save(order);
    }

    // 根据主键查
    public Optional<Order> findById(Integer id) {
        return orderMapper.findById(id);
    }

    // 根据订单名查
    public Optional<Order> findByOrderName(String name) {
        return orderMapper.findByOrderName(name);
    }

    // 根据菜品ID查
    public List<Order> listByDishId(Integer dishId) {
        return orderMapper.findByDish_DishId(dishId);
    }

    // 根据优先级查
    public List<Order> listByPriority(Integer priority) {
        return orderMapper.findByPriority(priority);
    }

    // 时间区间查
    public List<Order> listByCreateTimeRange(LocalDateTime start, LocalDateTime end) {
        return orderMapper.findByCreateTimeBetween(start, end);
    }

    // 按创建时间升序取全部
    public List<Order> listAllOrderByCreateTimeAsc() {
        return orderMapper.findAllByOrderByCreateTimeAsc();
    }

    // 按优先级降序取全部
    public List<Order> listAllOrderByPriorityDesc() {
        return orderMapper.findAllByOrderByPriorityDesc();
    }

    /**
     * 更新订单状态
     */
    public Order updateOrderStatus(Integer orderId, OrderStatus status) {
        Optional<Order> optionalOrder = orderMapper.findById(orderId);
        if (optionalOrder.isPresent()) {
            Order order = optionalOrder.get();
            order.setOrderStatus(status);
            if (status == OrderStatus.COMPLETED) {
                order.setCompleteTime(LocalDateTime.now());
            }
            return orderMapper.save(order);
        }
        return null;
    }
}
