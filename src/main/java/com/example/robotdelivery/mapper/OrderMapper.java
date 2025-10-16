package com.example.robotdelivery.mapper;

import com.example.robotdelivery.pojo.Order;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderMapper extends CrudRepository<Order,Integer> {
    // 按创建时间倒序查询所有订单
    List<Order> findAllByOrderByCreateTimeDesc();

    // 查询最近的N条订单
    @Query(value = "SELECT * FROM tb_order ORDER BY create_time DESC LIMIT ?1", nativeQuery = true)
    List<Order> findTopByOrderByCreateTimeDesc(int limit);
}
