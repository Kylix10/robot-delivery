package com.example.robotdelivery.mapper;

import com.example.robotdelivery.pojo.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderMapper extends JpaRepository<Order, Integer> {

    Optional<Order> findByOrderName(String orderName);

    List<Order> findByDish_DishId(Integer dishId);

    List<Order> findByPriority(Integer priority);

    List<Order> findByCreateTimeBetween(LocalDateTime start, LocalDateTime end);

    List<Order> findAllByOrderByCreateTimeAsc();

    List<Order> findAllByOrderByPriorityDesc();

    // 获取最新创建的10条订单（按创建时间倒序）
    List<Order> findTop10ByOrderByCreateTimeDesc();
    // 【新增/确认】查询所有订单，并按创建时间倒序排列
    List<Order> findAllByOrderByCreateTimeDesc();
}
