package com.example.robotdelivery.service;

import com.example.robotdelivery.pojo.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

@Service
public class OrderProduction {
    private Integer productionTime;
    private Order currentOrder;
    public void startOrderProduction( Order order){
        new Thread(() -> {
            try {
                System.out.println("订单开始制作:" + order.getOrderId());
                Thread.sleep(productionTime); // 增加模拟时间以便观察
                System.out.println("订单制作时间到:" + order.getOrderId());
                order.setOrderStatus(Order.OrderStatus.COMPLETED);
                currentOrder.setCompleteTime(LocalDateTime.now());
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

    }

}
