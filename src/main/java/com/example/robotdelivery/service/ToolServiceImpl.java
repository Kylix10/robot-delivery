package com.example.robotdelivery.service;

import com.example.robotdelivery.pojo.Tools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ToolService 的实现类，用于处理工具相关的业务逻辑和数据获取。
 */
@Service
public class ToolServiceImpl implements ToolService {
    // 新增：注入 ToolManager 或 ToolService
    @Autowired
    private ToolManager toolManager;

    /**
     * 实现接口方法：获取所有工具的列表（供 Controller 使用）
     * @return 存储在 ToolManager 中的所有 Tools 实例列表
     */
    @Override
    public List<Tools> getAllTools() {
        // Service 只需要调用 Manager 的方法，获取最新的工具状态列表
        return toolManager.getAllToolInstances();
    }

}