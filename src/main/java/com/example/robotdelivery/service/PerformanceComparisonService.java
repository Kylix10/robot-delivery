package com.example.robotdelivery.service;

import com.example.robotdelivery.mapper.PerformanceResultMapper;
import com.example.robotdelivery.pojo.Order;
import com.example.robotdelivery.pojo.PerformanceResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 性能对比服务：读内存订单→算指标→写数据库
 */
@Service
public class PerformanceComparisonService {
    @Autowired
    private PerformanceResultMapper performanceResultMapper;

    // 定时任务：每5秒计算一次（可根据需求调整间隔）
    @Scheduled(fixedRate = 5000)
    public void calcAndSavePerformance() {
        // 计算算法模式指标（读内存列表）
        calcAndSave(
                ResourceManagerThread.ALG_COMPLETED_ORDERS,
                1,
                "算法模式（ResourceManagerThread）"
        );

        // 计算默认模式指标（读内存列表）
        calcAndSave(
                DeadlockSimulation.COMPLETED_ORDERS,
                2,
                "默认模式（DeadlockSimulation）"
        );
    }

    /**
     * 通用计算+保存方法（核心逻辑）
     */
    private void calcAndSave(List<Order> completedOrders, int modeType, String modeName) {
        // 空列表直接跳过
        if (completedOrders == null || completedOrders.isEmpty()) {
            System.out.println("[" + modeName + "] 暂无已完成订单，跳过计算");
            return;
        }

        PerformanceResult result = new PerformanceResult();
        result.setModeType(modeType);
        int orderCount = completedOrders.size();
        result.setCompletedCount(orderCount);

        // 初始化指标变量
        double totalRevenue = 0.0;
        long totalResponseMs = 0;
        LocalDateTime firstCreate = null;
        LocalDateTime lastComplete = null;

        // 遍历订单计算明细（增加非空判断，避免空指针）
        for (Order order : completedOrders) {
            if (order == null) {
                System.err.println("[" + modeName + "] 发现空订单，跳过计算");
                continue;
            }

            // 1. 计算总收益（价格×优先级，严格非空判断）
            if (order.getDish() != null && order.getDish().getDish_price() != null && order.getPriority() != null) {
                totalRevenue += order.getDish().getDish_price() *(1+ order.getPriority()/10);
            } else {
                System.err.println("[" + modeName + "] 订单ID=" + (order.getOrderId() != null ? order.getOrderId() : "未知")
                        + " 缺少价格或优先级，收益计算可能不准确");
            }

            // 2. 计算总响应时间（创建→完成）
            if (order.getCreateTime() != null && order.getCompleteTime() != null) {
                totalResponseMs += Duration.between(order.getCreateTime(), order.getCompleteTime()).toMillis();
                // 记录第一个创建时间和最后一个完成时间
                if (firstCreate == null) {
                    firstCreate = order.getCreateTime();
                }
                lastComplete = order.getCompleteTime(); // 最后一个会覆盖前面的，确保是最后完成的
            } else {
                System.err.println("[" + modeName + "] 订单ID=" + (order.getOrderId() != null ? order.getOrderId() : "未知")
                        + " 缺少创建时间或完成时间，响应时间计算可能不准确");
            }
        }

        // 处理运行时间（避免空指针）
        long runTimeSec = 0;
        if (firstCreate != null && lastComplete != null) {
            runTimeSec = Duration.between(firstCreate, lastComplete).getSeconds();
        } else {
            System.err.println("[" + modeName + "] 无法计算运行时间（缺少订单时间信息）");
        }

        // 填充指标（避免除0异常）
        result.setTotalRevenue(totalRevenue);
        result.setAvgResponseTimeMs(orderCount > 0 ? (double) totalResponseMs / orderCount : 0);
        result.setThroughput(runTimeSec > 0 ? (double) orderCount / runTimeSec : 0);

        // 显式设置计算时间（确保不为空）
        result.setCalcTime(LocalDateTime.now());

        // 写入数据库
        try {
            performanceResultMapper.save(result);
            // 完善日志，输出所有关键指标
            System.out.println("[" + modeName + "] 性能结果已存库：" +
                    "完成订单数=" + orderCount + "，" +
                    "总收益=" + totalRevenue + "，" +
                    "平均响应时间(ms)=" + result.getAvgResponseTimeMs() + "，" +
                    "吞吐量(订单/秒)=" + result.getThroughput());
        } catch (Exception e) {
            System.err.println("[" + modeName + "] 性能结果写入数据库失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}