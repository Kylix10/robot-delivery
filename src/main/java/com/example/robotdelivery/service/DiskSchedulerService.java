package com.example.robotdelivery.service;

import com.example.robotdelivery.pojo.*;
import com.example.robotdelivery.vo.OrderScheduleResult;
import com.example.robotdelivery.vo.SchedulerResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DiskSchedulerService implements DiskSchedulerInterface {

    @Autowired
    private Warehouse warehouse; // 仓库内的食材坐标表

    @Override
    public OrderScheduleResult handleOrderSchedule(Order order) {
        if (order == null || order.getDish() == null) {
            System.out.println("❌ 无法调度：订单或菜品为空");
            return null;
        }

        Dish dish = order.getDish();
        System.out.println("🚚 【仓库调度】开始规划订单：" + order.getOrderId() +
                "（菜品：" + dish.getDishName() + "）所需食材路径");

        // 获取菜品所需的食材列表
        List<Ingredient> ingredients = dish.getIngredients();
        if (ingredients == null || ingredients.isEmpty()) {
            System.out.println("⚠️ 菜品未定义所需食材，跳过");
            return null;
        }

        // 收集食材在仓库的位置信息
        List<Integer> positions = new ArrayList<>();
        for (Ingredient ing : ingredients) {
            if (ing.getPosition() != null) {
                positions.add(ing.getPosition());
            } else {
                warehouse.getPositionByIngredientName(ing.getName())
                        .ifPresent(positions::add);
            }
        }

        if (positions.isEmpty()) {
            System.out.println("⚠️ 没有可用的食材位置信息");
            return null;
        }

        // 调用三种调度算法
        Map<String, SchedulerResult> results = new LinkedHashMap<>();
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

    // --------- 以下是三种磁盘调度算法 ------------
    private SchedulerResult scheduleFCFS(int current, List<Integer> reqs) {
        return schedule(current, reqs, Comparator.comparingInt(o -> reqs.indexOf(o)), "FCFS");
    }

    private SchedulerResult scheduleSSTF(int current, List<Integer> reqs) {
        return schedule(current, reqs, Comparator.comparingInt(o -> Math.abs(o - current)), "SSTF");
    }

    private SchedulerResult scheduleSCAN(int current, List<Integer> reqs) {
        List<Integer> sorted = new ArrayList<>(reqs);
        Collections.sort(sorted);
        return schedule(current, sorted, Comparator.naturalOrder(), "SCAN");
    }

    private SchedulerResult schedule(int current, List<Integer> reqs, Comparator<Integer> comparator, String alg) {
        SchedulerResult result = new SchedulerResult();
        result.setAlgorithmName(alg);

        List<Integer> remaining = new ArrayList<>(reqs);
        List<String> steps = new ArrayList<>();
        int total = 0;

        while (!remaining.isEmpty()) {
            remaining.sort(comparator);
            int next = remaining.remove(0);
            int dist = Math.abs(next - current);
            total += dist;
            String ingName = warehouse.getIngredientByPosition(next)
                    .map(Ingredient::getName)
                    .orElse("未知食材");
            steps.add(current + " → " + next + "（" + ingName + "） dist=" + dist);
            current = next;
        }

        result.setStepDetails(steps);
        result.setTotalDistance(total);
        return result;
    }

    // 新增：暴露仓库数据给 PlanningService
    public Map<Integer, String> listWarehouseIngredients() {
        Map<Integer, String> map = new HashMap<>();
        for (Map.Entry<Integer, Ingredient> entry : warehouse.getAllIngredients().entrySet()) {
            map.put(entry.getKey(), entry.getValue().getName());
        }
        return map;
    }

    // 新增：外部算法调用接口
    public SchedulerResult schedule(String algorithm, int current, List<Integer> reqs) {
        switch (algorithm.toUpperCase()) {
            case "FCFS":
                return scheduleFCFS(current, reqs);
            case "SSTF":
                return scheduleSSTF(current, reqs);
            case "SCAN":
                return scheduleSCAN(current, reqs);
            default:
                throw new IllegalArgumentException("未知算法: " + algorithm);
        }
    }

}
