package com.example.robotdelivery.mapper;

import com.example.robotdelivery.pojo.PerformanceResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerformanceResultMapper extends JpaRepository<PerformanceResult, Integer> {
    // 原有方法：按模式类型查询所有数据，按计算时间倒序（保留）
    List<PerformanceResult> findByModeTypeOrderByCalcTimeDesc(int modeType);

    // 新增方法1：查询算法模式（mode_type=1）最新1条数据
    // 核心逻辑：Top1表示只取1条，OrderByCalcTimeDesc确保取最新的
    PerformanceResult findTop1ByModeTypeOrderByCalcTimeDesc(int modeType);

    // 新增方法：查询最新 N 条历史数据（用于折线图，假设 N=20）
    List<PerformanceResult> findTop20ByModeTypeOrderByCalcTimeDesc(int modeType);
}