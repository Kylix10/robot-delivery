package com.example.robotdelivery.controller;

import com.example.robotdelivery.pojo.Warehouse;
import com.example.robotdelivery.service.DiskSchedulerService;
import com.example.robotdelivery.vo.SchedulerResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 仓库与调度算法控制器
 * * 专门用于对接 robot.html + restaurant.js 中的 "路径规划" (path-planning-page) 页面。
 * * 确保 @GetMapping 路径与 restaurant.js 中的 pathPlanningApi 对象完全一致。
 */
@RestController
@RequestMapping("/api") // 对应 restaurant.js 中的 API_BASE = 'http://localhost:8088/api'
@CrossOrigin(origins = "*") // 允许前端页面(通常从 file:// 或不同端口)访问
public class WarehouseController {

    @Autowired
    private Warehouse warehouse; // 用于获取所有食材位置

    @Autowired
    private DiskSchedulerService schedulerService; // 用于运行算法

    /**
     * 接口 1: 获取仓库所有食材及其位置
     * 对应 JS: pathPlanningApi.warehouse (`${API_BASE}/warehouse`)
     * 由 loadWarehouse() 函数调用。
     */
    @GetMapping("/warehouse") // <--- 修正后的路径
    public Map<Integer, String> getWarehouseIngredients() {
        // schedulerService.listWarehouseIngredients() 已经实现了此功能
        return schedulerService.listWarehouseIngredients();
    }

    /**
     * 接口 2: 生成随机的食材请求列表
     * 对应 JS: pathPlanningApi.random (`${API_BASE}/requests/random`)
     * 由 generateRequests() 函数调用。
     */
    @GetMapping("/requests/random") // <--- 修正后的路径
    public List<Integer> generateRandomRequests(@RequestParam(defaultValue = "6") int count) {

        // 从 Warehouse Bean 中获取所有位置
        List<Integer> allPositions = new ArrayList<>(warehouse.getAllIngredients().keySet());

        if (allPositions.isEmpty()) {
            return Collections.emptyList();
        }

        // 随机打乱
        Collections.shuffle(allPositions);

        // 截取指定数量
        return allPositions.stream().limit(count).collect(Collectors.toList());
    }

    /**
     * 接口 3: 运行指定的调度算法
     * 对应 JS: pathPlanningApi.schedule (`${API_BASE}/schedule`)
     * 由 runSchedule() 和 runCompare() 函数调用。
     *
     * @param algorithm       (来自 @RequestParam)
     * @param initialPosition (来自 @RequestParam)
     * @param requestPositions (来自 @RequestBody, 是一个 JSON 数组 [10, 25, 5])
     * @return SchedulerResult (完整的 JSON 对象，包含 processedOrder, stepDistances 等)
     */
    @PostMapping("/schedule") // <--- 此路径保持不变
    public SchedulerResult runScheduler(
            @RequestParam String algorithm,
            @RequestParam(defaultValue = "0") int initialPosition,
            @RequestBody List<Integer> requestPositions) {

        // 健壮性检查，防止前端发送空请求
        if (requestPositions == null || requestPositions.isEmpty()) {
            SchedulerResult emptyResult = new SchedulerResult();
            emptyResult.setAlgorithmName(algorithm);
            emptyResult.setTotalDistance(0);
            emptyResult.setProcessedOrder(Collections.emptyList());
            emptyResult.setStepDistances(Collections.emptyList());
            emptyResult.setStepDetails(List.of("没有需要调度的请求。"));
            return emptyResult;
        }

        try {
            // 直接调用 service 方法。
            // (请确保您使用的是我之前提供的 DiskSchedulerService.java 版本，
            // 它能正确返回前端动画所需的 processedOrder 和 stepDistances 字段。)
            return schedulerService.schedule(
                    algorithm,
                    initialPosition,
                    requestPositions
            );
        } catch (IllegalArgumentException e) {
            // 捕获 "未知算法" 异常
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}