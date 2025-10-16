package com.example.robotdelivery.service;

import com.example.robotdelivery.pojo.Memory;
import com.example.robotdelivery.pojo.vo.MemoryVO;
import com.example.robotdelivery.pojo.vo.MemoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field; // 可以移除此 import
import java.util.stream.Collectors; // 可以移除此 import

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

    // ... 其他 service 方法（如操作方法） ...
}