package com.example.robotdelivery.service;

import com.example.robotdelivery.mapper.OrderMapper;
import com.example.robotdelivery.pojo.Order;
import com.example.robotdelivery.pojo.dto.OrderDto;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
@Service
public class OrderService implements IOrderService{

    @Autowired
    OrderMapper orderMapper;

    // 保存订单
    @Transactional
    public Order saveOrder(Order order) {
        // 可以添加业务逻辑，如设置默认值、验证等
        if (order.getCreateTime() == null) {
            order.setCreateTime(LocalDateTime.now());
        }
        return orderMapper.save(order); // 调用OrderMapper的save方法
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

    @Override
    public List<Order> findAll() {
        // 查询所有订单，按创建时间倒序排列
        return orderMapper.findAllByOrderByCreateTimeDesc();
    }

    @Override
    public List<Order> findRecentOrders(int limit) {
        // 查询最近的N条订单
        return orderMapper.findTopByOrderByCreateTimeDesc(limit);
    }



}
