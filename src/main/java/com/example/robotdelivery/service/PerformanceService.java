package com.example.robotdelivery.service;

import com.example.robotdelivery.mapper.PerformanceResultMapper;
import com.example.robotdelivery.pojo.Order;
import com.example.robotdelivery.pojo.PerformanceResult;
import com.example.robotdelivery.pojo.dto.PerformanceComparisonDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
@Service
public class PerformanceService implements IPerformanceService{
    @Autowired
    private PerformanceResultMapper performanceResultMapper;

    // 核心修改：用JPA新方法查询最新数据
    @Override
    public PerformanceComparisonDTO getLatestPerformanceComparison() {
        PerformanceComparisonDTO comparisonDTO = new PerformanceComparisonDTO();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try {
            // 1. 查询最新数据（原有逻辑不变）
            PerformanceResult algorithmResult = performanceResultMapper.findTop1ByModeTypeOrderByCalcTimeDesc(1);
            PerformanceResult defaultResult = performanceResultMapper.findTop1ByModeTypeOrderByCalcTimeDesc(2);

            // 2. 查询历史数据（原有逻辑不变）
            List<PerformanceResult> algorithmHistoryResults = performanceResultMapper.findTop20ByModeTypeOrderByCalcTimeDesc(1);
            List<PerformanceResult> defaultHistoryResults = performanceResultMapper.findTop20ByModeTypeOrderByCalcTimeDesc(2);

            // -------------------------- 新增：历史数据转换与赋值 --------------------------
            // 2.1 转换“算法模式”历史数据为 ModePerformanceDTO 列表
            List<PerformanceComparisonDTO.ModePerformanceDTO> algorithmHistoryDTOs = new ArrayList<>();
            for (PerformanceResult result : algorithmHistoryResults) {
                PerformanceComparisonDTO.ModePerformanceDTO dto = new PerformanceComparisonDTO.ModePerformanceDTO();
                dto.setCompletedCount(result.getCompletedCount());
                dto.setTotalRevenue(result.getTotalRevenue());
                dto.setAvgResponseTimeMs(result.getAvgResponseTimeMs());
                dto.setThroughput(result.getThroughput());
                dto.setCalcTime(result.getCalcTime().format(formatter));
                algorithmHistoryDTOs.add(dto);
            }

            // 2.2 转换“默认模式”历史数据为 ModePerformanceDTO 列表
            List<PerformanceComparisonDTO.ModePerformanceDTO> defaultHistoryDTOs = new ArrayList<>();
            for (PerformanceResult result : defaultHistoryResults) {
                PerformanceComparisonDTO.ModePerformanceDTO dto = new PerformanceComparisonDTO.ModePerformanceDTO();
                dto.setCompletedCount(result.getCompletedCount());
                dto.setTotalRevenue(result.getTotalRevenue());
                dto.setAvgResponseTimeMs(result.getAvgResponseTimeMs());
                dto.setThroughput(result.getThroughput());
                dto.setCalcTime(result.getCalcTime().format(formatter));
                defaultHistoryDTOs.add(dto);
            }

            // 2.3 将转换后的历史数据列表赋值给 DTO
            comparisonDTO.setAlgorithmHistory(algorithmHistoryDTOs);
            comparisonDTO.setDefaultHistory(defaultHistoryDTOs);
            // --------------------------------------------------------------------------

            // 3. 转换最新数据为 DTO（原有逻辑不变）
            PerformanceComparisonDTO.ModePerformanceDTO algorithmModeDTO = new PerformanceComparisonDTO.ModePerformanceDTO();
            if (algorithmResult != null) {
                algorithmModeDTO.setCompletedCount(algorithmResult.getCompletedCount());
                algorithmModeDTO.setTotalRevenue(algorithmResult.getTotalRevenue());
                algorithmModeDTO.setAvgResponseTimeMs(algorithmResult.getAvgResponseTimeMs());
                algorithmModeDTO.setThroughput(algorithmResult.getThroughput());
                algorithmModeDTO.setCalcTime(algorithmResult.getCalcTime().format(formatter));
            }

            PerformanceComparisonDTO.ModePerformanceDTO defaultModeDTO = new PerformanceComparisonDTO.ModePerformanceDTO();
            if (defaultResult != null) {
                defaultModeDTO.setCompletedCount(defaultResult.getCompletedCount());
                defaultModeDTO.setTotalRevenue(defaultResult.getTotalRevenue());
                defaultModeDTO.setAvgResponseTimeMs(defaultResult.getAvgResponseTimeMs());
                defaultModeDTO.setThroughput(defaultResult.getThroughput());
                defaultModeDTO.setCalcTime(defaultResult.getCalcTime().format(formatter));
            }

            comparisonDTO.setAlgorithmMode(algorithmModeDTO);
            comparisonDTO.setDefaultMode(defaultModeDTO);
            comparisonDTO.setStatus("success");
            comparisonDTO.setMessage("数据查询成功");

        } catch (Exception e) {
            comparisonDTO.setStatus("error");
            comparisonDTO.setMessage("数据查询失败：" + e.getMessage());
            e.printStackTrace();
        }

        return comparisonDTO;
    }
}
