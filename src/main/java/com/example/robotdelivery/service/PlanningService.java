package com.example.robotdelivery.service;

import com.example.robotdelivery.pojo.Order;
import com.example.robotdelivery.mapper.OrderMapper;
import com.example.robotdelivery.vo.SchedulerResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PlanningService {

    private final OrderMapper orderMapper;
    private final DiskSchedulerService diskSchedulerService;

    public PlanningService(OrderMapper orderMapper, DiskSchedulerService diskSchedulerService) {
        this.orderMapper = orderMapper;
        this.diskSchedulerService = diskSchedulerService;
    }

    //@Scheduled(fixedRate = 10000, initialDelay = 3000)
    public void planForLatestOrders() {
        List<Order> latest = orderMapper.findTop10ByOrderByCreateTimeDesc();
        if (latest.isEmpty()) {
            System.out.println("[规划] 暂无订单可规划");
            return;
        }

        Map<Integer, String> warehouseMap = diskSchedulerService.listWarehouseIngredients();
        if (warehouseMap.isEmpty()) {
            System.out.println("[规划] 仓库暂无食材位置数据");
            return;
        }

        List<Integer> candidatePositions = new ArrayList<>(warehouseMap.keySet());
        Collections.sort(candidatePositions);

        for (Order order : latest) {
            if (order.getDish() == null || order.getDish().getDishId() == null) continue;

            int key = order.getDish().getDishId() * 5;
            int mappedPosition = nearest(candidatePositions, key);

            List<Integer> requestPositions = new ArrayList<>();
            requestPositions.add(mappedPosition);

            int initialPosition = 0;
            SchedulerResult fcfs = diskSchedulerService.schedule("FCFS", initialPosition, requestPositions);
            SchedulerResult sstf = diskSchedulerService.schedule("SSTF", initialPosition, requestPositions);
            SchedulerResult scan = diskSchedulerService.schedule("SCAN", initialPosition, requestPositions);

            SchedulerResult best = fcfs;
            if (sstf.getTotalDistance() < best.getTotalDistance()) best = sstf;
            if (scan.getTotalDistance() < best.getTotalDistance()) best = scan;

            System.out.println("================= 订单路径规划 =================");
            System.out.println("订单ID=" + order.getOrderId() + " 菜品=" + order.getDish().getDishName());
            System.out.println("请求位置=" + requestPositions);
            print("FCFS", fcfs);
            print("SSTF", sstf);
            print("SCAN", scan);
            System.out.println("[最优算法] " + best.getAlgorithmName() + " 总距离=" + best.getTotalDistance());
            System.out.println("==============================================");
        }
    }

    private int nearest(List<Integer> sortedPositions, int key) {
        int idx = Collections.binarySearch(sortedPositions, key);
        if (idx >= 0) return sortedPositions.get(idx);
        int insertPoint = -idx - 1;
        if (insertPoint <= 0) return sortedPositions.get(0);
        if (insertPoint >= sortedPositions.size()) return sortedPositions.get(sortedPositions.size() - 1);
        int low = sortedPositions.get(insertPoint - 1);
        int high = sortedPositions.get(insertPoint);
        return Math.abs(low - key) <= Math.abs(high - key) ? low : high;
    }

    private void print(String name, SchedulerResult r) {
        System.out.println("[" + name + "] 总距离=" + r.getTotalDistance());
        System.out.println("顺序=" + r.getProcessedOrder());
        if (r.getStepDetails() != null) {
            for (String s : r.getStepDetails()) {
                System.out.println(s);
            }
        }
    }
}
