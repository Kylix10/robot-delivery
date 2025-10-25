package com.example.robotdelivery.service;

import com.example.robotdelivery.pojo.Memory;
import com.example.robotdelivery.pojo.Partition;
import com.example.robotdelivery.pojo.vo.MemoryVO;
import com.example.robotdelivery.pojo.vo.MemoryVO;
import com.example.robotdelivery.pojo.vo.WorkstationVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field; // 可以移除此 import
import java.util.List;
import java.util.stream.Collectors; // 可以移除此 import
import java.util.stream.IntStream;

/**
 * 内存/工作台服务类：提供状态查询和操作接口
 */
@Service
public class MemoryService {

    private final MemoryManager memoryManager;

    @Autowired // 依赖注入 MemoryManager
    public MemoryService(MemoryManager memoryManager) {
        // 确保在 Spring 环境下 MemoryManager 能正确初始化和管理状态
        this.memoryManager = memoryManager;
    }

    /**
     * 将 MemoryManager 的当前状态封装为 MemoryVO
     * 供前端查询和可视化使用。
     * @return MemoryVO 包含总览信息和分区列表
     */
    public MemoryVO getMemoryStatus() {
        MemoryVO vo = new MemoryVO();
        Memory memory = memoryManager.getMemory(); // 获取 Memory POJO

        // 1. 填充总览信息
        vo.setTotalSpace(memory.getTotalSpace());
        vo.setUsedSpace(memory.getUsedSpace());
        vo.setFreeSpace(memory.getFreeSpace());

        // 2. 填充分区列表
        // 【修改点】：直接调用 MemoryManager 中新增的公共方法
        vo.setPartitions(memoryManager.getPartitions());

        return vo;
    }

    /**
     * 获取工作台分区的详细表格视图数据
     * 专门用于前端表格（视图 B）
     * @return List<WorkstationVo> 包含每个分区的详细状态
     */
    // MemoryService 中 getWorkstationDetails 方法修改
    public List<WorkstationVo> getWorkstationDetails() {
        Memory memory = memoryManager.getMemory();
        List<Partition> partitions = memory.getPartitions();

        return IntStream.range(0, partitions.size())
                .mapToObj(index -> WorkstationVo.fromMemory(memory, index))
                .collect(Collectors.toList());
    }
}