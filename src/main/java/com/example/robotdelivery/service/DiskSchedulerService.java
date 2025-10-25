package com.example.robotdelivery.service;

import com.example.robotdelivery.pojo.*;
import com.example.robotdelivery.vo.OrderScheduleResult;
import com.example.robotdelivery.vo.SchedulerResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DiskSchedulerService implements DiskSchedulerInterface{

    @Autowired
    private Warehouse warehouse; // 仓库内的食材坐标表

    /**
     * 处理订单调度（内部使用，用于订单整体分析对比）
     */
    public OrderScheduleResult handleOrderSchedule(Order order) {
        if (order == null || order.getDish() == null) {
            System.out.println("❌ 无法调度：订单或菜品为空");
            return null;
        }

        Dish dish = order.getDish();
        System.out.println("🚚 【仓库调度】开始规划订单：" + order.getOrderId() +
                "（菜品：" + dish.getDishName() + "）所需食材路径");

        // 收集食材在仓库的位置信息
        List<Integer> positions = dish.getIngredients().stream()
                .map(ing -> {
                    if (ing.getPosition() != null) return ing.getPosition();
                    return warehouse.getPositionByIngredientName(ing.getName()).orElse(null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (positions.isEmpty()) {
            System.out.println("⚠️ 没有可用的食材位置信息");
            return null;
        }

        // 调用三种调度算法
        Map<String, SchedulerResult> results = new LinkedHashMap<>();
        // 初始位置固定为 0
        results.put("FCFS", scheduleFCFS(0, positions));
        results.put("SSTF", scheduleSSTF(0, positions));
        results.put("SCAN", scheduleSCAN(0, positions));

        // 打印结果
        for (Map.Entry<String, SchedulerResult> e : results.entrySet()) {
            System.out.println("🔹 算法：" + e.getKey() + " 总路径距离：" + e.getValue().getTotalDistance());
            e.getValue().getStepDetails().forEach(step -> System.out.println("   " + step));
        }

        // 组装返回结果
        OrderScheduleResult result = new OrderScheduleResult();
        result.setDishName(dish.getDishName());
        result.setAlgorithmResults(results);
        return result;
    }


    /**
     * 暴露给前端 API 的统一调度接口
     * @param algorithm 算法名称
     * @param current 初始位置
     * @param reqs 请求位置列表
     * @return 调度结果
     */
    public SchedulerResult schedule(String algorithm, int current, List<Integer> reqs) {
        // 对请求列表进行一次拷贝，避免修改原始列表
        List<Integer> requests = new ArrayList<>(reqs);

        switch (algorithm.toUpperCase()) {
            case "FCFS":
                return scheduleFCFS(current, requests);
            case "SSTF":
                return scheduleSSTF(current, requests);
            case "SCAN":
                return scheduleSCAN(current, requests);
            default:
                throw new IllegalArgumentException("未知算法: " + algorithm);
        }


    }

    // --------- FCFS 调度算法 ------------
    private SchedulerResult scheduleFCFS(int current, List<Integer> reqs) {
        // FCFS 顺序就是请求到达的顺序，reqs 已经是原始顺序
        return commonSchedule(current, reqs, "FCFS");
    }

    // --------- SSTF 调度算法 ------------
    private SchedulerResult scheduleSSTF(int current, List<Integer> reqs) {
        // SSTF 需要在每次迭代时找到最近的请求
        List<Integer> remaining = new ArrayList<>(reqs);
        List<Integer> processedOrder = new ArrayList<>();
        List<Integer> stepDistances = new ArrayList<>();
        List<String> steps = new ArrayList<>();
        int total = 0;

        while (!remaining.isEmpty()) {
            // 找到离 current 最近的请求
            int bestIndex = 0;
            int minDistance = Integer.MAX_VALUE;
            int next = -1;

            for (int i = 0; i < remaining.size(); i++) {
                int req = remaining.get(i);
                int dist = Math.abs(req - current);
                if (dist < minDistance) {
                    minDistance = dist;
                    next = req;
                    bestIndex = i;
                }
            }

            if (next == -1) break; // 理论上不会发生

            remaining.remove(bestIndex);

            total += minDistance;
            processedOrder.add(next);
            stepDistances.add(minDistance);

            String ingName = warehouse.getIngredientByPosition(next)
                    .map(Ingredient::getName)
                    .orElse("未知食材");

            steps.add(current + " → " + next + "（" + ingName + "） dist=" + minDistance);
            current = next;
        }

        return createResult("SSTF", processedOrder, stepDistances, total, steps);
    }

    // --------- SCAN 调度算法 ------------
    private SchedulerResult scheduleSCAN(int current, List<Integer> reqs) {
        // 假设 SCAN 始终向右（坐标增大方向）移动，直到最大位置，然后返回到最小位置
        List<Integer> sorted = new ArrayList<>(reqs);
        Collections.sort(sorted);

        List<Integer> lower = sorted.stream().filter(p -> p < current).collect(Collectors.toList());
        List<Integer> upper = sorted.stream().filter(p -> p >= current).collect(Collectors.toList());

        // SCAN 总是先向右移动：Upper 组（升序） -> Lower 组（降序）
        List<Integer> scanOrder = new ArrayList<>();
        scanOrder.addAll(upper); // 向右移动
        Collections.reverse(lower); // 向左移动时，处理较近的请求
        scanOrder.addAll(lower);

        // 注意：SCAN算法通常需要知道仓库的最大和最小边界，这里简化为只在请求范围内移动。

        return commonSchedule(current, scanOrder, "SCAN");
    }

    /**
     * 核心调度逻辑（适用于 FCFS 和已预先排序的 SCAN）
     * @param current 初始位置
     * @param orderedReqs 已经按照算法要求排序的请求列表
     * @param alg 算法名称
     * @return 调度结果
     */
    private SchedulerResult commonSchedule(int current, List<Integer> orderedReqs, String alg) {
        List<Integer> processedOrder = new ArrayList<>();
        List<Integer> stepDistances = new ArrayList<>();
        List<String> steps = new ArrayList<>();
        int total = 0;

        for (int next : orderedReqs) {
            int dist = Math.abs(next - current);
            total += dist;
            processedOrder.add(next);
            stepDistances.add(dist);

            String ingName = warehouse.getIngredientByPosition(next)
                    .map(Ingredient::getName)
                    .orElse("未知食材");

            steps.add(current + " → " + next + "（" + ingName + "） dist=" + dist);
            current = next;
        }

        return createResult(alg, processedOrder, stepDistances, total, steps);
    }

    /**
     * 结果封装辅助方法
     */
    private SchedulerResult createResult(String alg, List<Integer> processedOrder, List<Integer> stepDistances, int total, List<String> steps) {
        SchedulerResult result = new SchedulerResult();
        result.setAlgorithmName(alg);
        result.setProcessedOrder(processedOrder);
        result.setStepDistances(stepDistances);
        result.setTotalDistance(total);
        result.setStepDetails(steps);
        return result;
    }

    /** * 暴露仓库数据给 PlanningService / Controller
     */
    public Map<Integer, String> listWarehouseIngredients() {
        Map<Integer, String> map = new HashMap<>();
        for (Map.Entry<Integer, Ingredient> entry : warehouse.getAllIngredients().entrySet()) {
            map.put(entry.getKey(), entry.getValue().getName());
        }
        return map;
    }
}