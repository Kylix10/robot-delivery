package com.example.robotdelivery.controller;

import com.example.robotdelivery.mapper.DishRepository;
import com.example.robotdelivery.pojo.Dish;
import com.example.robotdelivery.service.DiskSchedulerService;
import com.example.robotdelivery.vo.OrderScheduleResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order-scheduler")
public class OrderSchedulerController {

    @Autowired
    private DiskSchedulerService schedulerService;

    @Autowired
    private DishRepository dishRepository;

    /** 单个订单调度 */
    @GetMapping("/dish/{dishId}")
    public OrderScheduleResult scheduleDish(
            @PathVariable Integer dishId,
            @RequestParam(defaultValue = "0") int initialPosition
    ) {
        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new RuntimeException("菜品不存在: " + dishId));
        return schedulerService.scheduleForOrder(dish, initialPosition);
    }
}
