package com.example.robotdelivery.controller;

import com.example.robotdelivery.pojo.dto.RobotDto;
import com.example.robotdelivery.service.IRobotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/robots")
public class RobotController {

    @Autowired
    private IRobotService robotService;

    @GetMapping
    public List<RobotDto> getAllRobots() {
        return robotService.getAllRobots();
    }

    @GetMapping("/{id}")
    public RobotDto getRobotById(@PathVariable Integer id) {
        return robotService.getRobotById(id);
    }
}