package com.example.robotdelivery.mapper;
//用来比较算法性能的类的接口
import com.example.robotdelivery.pojo.PerformanceResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerformanceResultMapper extends JpaRepository<PerformanceResult, Integer> {
    List<PerformanceResult> findByModeTypeOrderByCalcTimeDesc(int i);
    // 无需额外方法，JpaRepository自带保存、查询等基础方法
}
