package com.example.robotdelivery.service;

import com.example.robotdelivery.pojo.Dish;
import com.example.robotdelivery.pojo.Ingredient;
import com.example.robotdelivery.pojo.Warehouse;
import com.example.robotdelivery.vo.OrderScheduleResult;
import com.example.robotdelivery.vo.SchedulerResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DiskSchedulerService {

    @Autowired
    private Warehouse warehouse;

    /** 通用调度接口 */
    public SchedulerResult schedule(String algorithm, int currentHeadPosition, List<Integer> requestPositions) {
        SchedulerResult result = new SchedulerResult();
        // 不用 stream
        Map<Integer, String> warehouseMap = new HashMap<>();
        for (Map.Entry<Integer, Ingredient> entry : warehouse.getAllIngredients().entrySet()) {
            warehouseMap.put(entry.getKey(), entry.getValue().getName());
        }
        result.setWarehouseIngredients(warehouseMap);

        if ("SSTF".equals(algorithm)) return scheduleSSTF(currentHeadPosition, requestPositions);
        if ("SCAN".equals(algorithm)) return scheduleSCAN(currentHeadPosition, requestPositions);
        return scheduleFCFS(currentHeadPosition, requestPositions);
    }

    /** 为单个订单调度其菜品食材，并比较三种算法 */
    public OrderScheduleResult scheduleForOrder(Dish dish, int initialPosition) {
        List<Integer> positions = new ArrayList<>();
        for (Ingredient ing : dish.getIngredients()) {
            positions.add(ing.getPosition());
        }

        Map<String, SchedulerResult> results = new LinkedHashMap<>();
        results.put("FCFS", schedule("FCFS", initialPosition, positions));
        results.put("SSTF", schedule("SSTF", initialPosition, positions));
        results.put("SCAN", schedule("SCAN", initialPosition, positions));

        OrderScheduleResult orderResult = new OrderScheduleResult();
        orderResult.setDishName(dish.getDishName());
        orderResult.setAlgorithmResults(results);
        return orderResult;
    }

    // ========== FCFS ==========
    private SchedulerResult scheduleFCFS(int currentHeadPosition, List<Integer> requestPositions) {
        return scheduleByOrder(currentHeadPosition, requestPositions, "FCFS");
    }

    // ========== SSTF ==========
    private SchedulerResult scheduleSSTF(int currentHeadPosition, List<Integer> requestPositions) {
        return scheduleByOrder(currentHeadPosition, requestPositions, "SSTF");
    }

    // ========== SCAN ==========
    private SchedulerResult scheduleSCAN(int currentHeadPosition, List<Integer> requestPositions) {
        return scheduleByOrder(currentHeadPosition, requestPositions, "SCAN");
    }

    /** 通用实现封装（内部调用） */
    private SchedulerResult scheduleByOrder(int currentPos, List<Integer> positions, String algorithm) {
        SchedulerResult result = new SchedulerResult();
        result.setAlgorithmName(algorithm);
        List<Integer> processedOrder = new ArrayList<>();
        List<Integer> stepDistances = new ArrayList<>();
        List<String> stepDetails = new ArrayList<>();
        int totalDistance = 0;

        List<Integer> remaining = new ArrayList<>(positions);

        if ("FCFS".equals(algorithm)) {
            for (Integer targetPos : remaining) {
                int dist = Math.abs(targetPos - currentPos);
                stepDistances.add(dist);
                totalDistance += dist;
                processedOrder.add(targetPos);
                String ingName = warehouse.getIngredientByPosition(targetPos).get() != null
                        ? warehouse.getIngredientByPosition(targetPos).get().getName() : "未知";
                stepDetails.add(currentPos + " -> " + targetPos + ", dist=" + dist + " (" + ingName + ")");
                currentPos = targetPos;
            }
        } else if ("SSTF".equals(algorithm)) {
            while (!remaining.isEmpty()) {
                int nearest = remaining.get(0);
                int minDist = Math.abs(nearest - currentPos);
                for (int i = 1; i < remaining.size(); i++) {
                    int dist = Math.abs(remaining.get(i) - currentPos);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = remaining.get(i);
                    }
                }
                int dist = Math.abs(nearest - currentPos);
                stepDistances.add(dist);
                totalDistance += dist;
                processedOrder.add(nearest);
                String ingName = warehouse.getIngredientByPosition(nearest).get() != null
                        ? warehouse.getIngredientByPosition(nearest).get().getName() : "未知";
                stepDetails.add(currentPos + " -> " + nearest + ", dist=" + dist + " (" + ingName + ")");
                currentPos = nearest;
                remaining.remove((Integer) nearest);
            }
        } else if ("SCAN".equals(algorithm)) {
            List<Integer> lower = new ArrayList<>();
            List<Integer> higher = new ArrayList<>();
            for (Integer pos : remaining) {
                if (pos < currentPos) lower.add(pos);
                else higher.add(pos);
            }
            Collections.sort(higher);
            Collections.sort(lower, Collections.reverseOrder());

            for (Integer targetPos : higher) {
                int dist = Math.abs(targetPos - currentPos);
                stepDistances.add(dist);
                totalDistance += dist;
                processedOrder.add(targetPos);
                String ingName = warehouse.getIngredientByPosition(targetPos).get() != null
                        ? warehouse.getIngredientByPosition(targetPos).get().getName() : "未知";
                stepDetails.add(currentPos + " -> " + targetPos + ", dist=" + dist + " (" + ingName + ")");
                currentPos = targetPos;
            }
            for (Integer targetPos : lower) {
                int dist = Math.abs(targetPos - currentPos);
                stepDistances.add(dist);
                totalDistance += dist;
                processedOrder.add(targetPos);
                String ingName = warehouse.getIngredientByPosition(targetPos).get() != null
                        ? warehouse.getIngredientByPosition(targetPos).get().getName() : "未知";
                stepDetails.add(currentPos + " -> " + targetPos + ", dist=" + dist + " (" + ingName + ")");
                currentPos = targetPos;
            }
        }

        result.setProcessedOrder(processedOrder);
        result.setStepDistances(stepDistances);
        result.setTotalDistance(totalDistance);
        result.setStepDetails(stepDetails);
        return result;
    }

    /** 返回仓库所有食材（位置->名称） */
    public Map<Integer, String> listWarehouseIngredients() {
        Map<Integer, String> map = new HashMap<>();
        for (Map.Entry<Integer, Ingredient> entry : warehouse.getAllIngredients().entrySet()) {
            map.put(entry.getKey(), entry.getValue().getName());
        }
        return map;
    }
}
