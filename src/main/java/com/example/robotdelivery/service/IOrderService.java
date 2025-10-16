package com.example.robotdelivery.service;

import com.example.robotdelivery.pojo.Order;
import com.example.robotdelivery.pojo.dto.OrderDto;

import java.util.List;

public interface IOrderService {
    void add(OrderDto order);

    // 添加查询方法
    List<Order> findAll();

    // 可以根据需要添加更多查询方法，例如按状态、时间等查询
    List<Order> findRecentOrders(int limit);
}
