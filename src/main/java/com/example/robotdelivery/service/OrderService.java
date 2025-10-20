package com.example.robotdelivery.service;

import com.example.robotdelivery.mapper.DishMapper;
import com.example.robotdelivery.mapper.OrderMapper;
import com.example.robotdelivery.pojo.Dish;
import com.example.robotdelivery.pojo.Order;
import com.example.robotdelivery.pojo.Order.OrderStatus;
import org.springframework.scheduling.annotation.Scheduled;
import com.example.robotdelivery.pojo.dto.OrderDto;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class OrderService implements IOrderService{

    @Autowired

    private final OrderMapper orderMapper;


    public OrderService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;

    }

    @Override
    public void add(OrderDto order) {
        // 调用数据访问类
        Order orderPojo = new Order();
        BeanUtils.copyProperties(order, orderPojo);
        // 设置创建时间
        orderPojo.setCreateTime(LocalDateTime.now());
        orderMapper.save(orderPojo);
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
