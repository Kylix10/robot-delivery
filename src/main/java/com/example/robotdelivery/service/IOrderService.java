package com.example.robotdelivery.service;

import com.example.robotdelivery.pojo.Order;
import com.example.robotdelivery.pojo.dto.OrderDto;
import com.example.robotdelivery.pojo.vo.OrderVO;

import java.util.List;
import java.util.Optional;

public interface IOrderService {
    void add(OrderDto order);

    // ================= 前端展示相关方法 (返回 OrderVO) =================

    /**
     * 获取所有订单的视图对象列表，通常按时间倒序排列。
     */
    List<OrderVO> getAllOrderVOs();

    /**
     * 获取最近 N 条订单的视图对象列表。
     */
    List<OrderVO> findRecentOrderVOs(int limit);

    // ================= 内部业务/数据查询方法 (返回 Order 实体) =================

    /**
     * 查询所有订单实体，通常按时间倒序排列。
     */
    List<Order> findAll();

    /**
     * 查询最近 N 条订单实体。
     */
    List<Order> findRecentOrders(int limit);

    /**
     * 根据订单ID查询订单实体。
     */
    Optional<Order> findById(Integer id);


    // 如果其他 Service 组件或Controller需要这些方法，也一并添加
    // Optional<Order> findByOrderName(String name);
    // List<Order> listByDishId(Integer dishId);
    // List<Order> listByPriority(Integer priority);
    // List<Order> listByCreateTimeRange(LocalDateTime start, LocalDateTime end);
}
