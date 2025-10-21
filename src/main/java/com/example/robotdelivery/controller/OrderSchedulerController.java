package com.example.robotdelivery.controller;
import com.example.robotdelivery.mapper.DishRepository;
import com.example.robotdelivery.service.DiskSchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order-scheduler")
public class OrderSchedulerController {

    @Autowired
    private DiskSchedulerService schedulerService;

    @Autowired
    private DishRepository dishRepository;

}
