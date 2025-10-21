package com.example.robotdelivery.controller;

import com.example.robotdelivery.pojo.Order;
import com.example.robotdelivery.pojo.dto.OrderDto;
import com.example.robotdelivery.service.IOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController// 接口方法返回对象 转换为json文本
@RequestMapping("/order")// localhost:8088/order
public class OrderController {
    //REST
    //增加
    @Autowired
    IOrderService orderService;

    @PostMapping //URL:localhost:8088/order method:post
    public String add(@RequestBody OrderDto order){
        orderService.add(order);
        return "success!";
    }
    //查询

    // 查询最近的N条订单，默认10条
    @GetMapping("/recent")
    public List<Order> getRecentOrders(@RequestParam(defaultValue = "10") int limit) {
        return orderService.findRecentOrders(limit);
    }

}
