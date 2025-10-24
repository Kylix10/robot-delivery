package com.example.robotdelivery.service;

import com.example.robotdelivery.mapper.PerformanceResultMapper;
import com.example.robotdelivery.pojo.dto.PerformanceComparisonDTO;
import com.example.robotdelivery.pojo.vo.PerformanceResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class PerformanceComparisonService {
    @Autowired
    private PerformanceResultMapper performanceResultMapper; // 或JPA的Repository

    // 聚合两种模式的最新性能数据
    public PerformanceComparisonDTO getLatestComparisonData() {
        PerformanceComparisonDTO result = new PerformanceComparisonDTO();

        // 1. 查询算法模式（mode_type=1）的最新数据
        List<PerformanceResult> algorithmResults = performanceResultMapper.findByModeTypeOrderByCalcTimeDesc(1);
        // 2. 查询默认模式（mode_type=2）的最新数据
        List<PerformanceResult> defaultResults = performanceResultMapper.findByModeTypeOrderByCalcTimeDesc(2);

        // 3. 处理数据（封装到DTO）
        result.setAlgorithmMode(convertToModeDTO(algorithmResults));
        result.setDefaultMode(convertToModeDTO(defaultResults));

        // 4. 判断状态（是否有有效数据）
        if (result.getAlgorithmMode() == null && result.getDefaultMode() == null) {
            result.setStatus("error");
            result.setMessage("暂无两种模式的性能数据，请先运行测试");
        } else if (result.getAlgorithmMode() == null) {
            result.setStatus("error");
            result.setMessage("缺少算法模式数据");
        } else if (result.getDefaultMode() == null) {
            result.setStatus("error");
            result.setMessage("缺少默认模式数据");
        } else {
            result.setStatus("success");
            result.setMessage("数据查询成功");
        }

        return result;
    }

    // 辅助方法：将数据库实体转为ModePerformanceDTO（取最新一条）
    private PerformanceComparisonDTO.ModePerformanceDTO convertToModeDTO(List<PerformanceResult> results) {
        if (results == null || results.isEmpty()) {
            return null;
        }
        // 取最新一条数据（已按calc_time倒序）
        PerformanceResult latest = results.get(0);
        PerformanceComparisonDTO.ModePerformanceDTO dto = new PerformanceComparisonDTO.ModePerformanceDTO();
        dto.setCompletedCount(latest.getCompletedCount());
        dto.setTotalRevenue(latest.getTotalRevenue());
        dto.setAvgResponseTimeMs(latest.getAvgResponseTimeMs());
        dto.setThroughput(latest.getThroughput());
        dto.setCalcTime(latest.getCalcTime().toString()); // 时间转字符串，前端直接展示
        return dto;
    }
}