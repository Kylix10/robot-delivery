package com.example.robotdelivery.service;

import com.example.robotdelivery.pojo.Order;
import com.example.robotdelivery.vo.OrderScheduleResult;

public interface DiskSchedulerInterface {
    /** 根据订单中的菜品，执行仓库路径规划（磁盘调度算法） */
    OrderScheduleResult handleOrderSchedule(Order order);

}

