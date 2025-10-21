package com.example.robotdelivery.service;

import com.example.robotdelivery.pojo.Tools;
import com.example.robotdelivery.pojo.Tools.ToolType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ToolManager {
    // 负责存储和持有工具的 List
    private final List<Tools> allTools = new ArrayList<>();
    private final AtomicInteger toolIdCounter = new AtomicInteger(1);

    @PostConstruct // 确保 Spring 注入完成后执行初始化
    public void initTools() {
        //原 ResourceManagerThread 中的初始化逻辑移到这里
        addTool(ToolType.OVEN, 2);    // 2个烤箱
        addTool(ToolType.FRY_PAN, 2); // 2个煎锅
        addTool(ToolType.FRY_POT, 1); // 1个炸锅
        System.out.println("[ToolManager] 烹饪工具初始化完成，总数: " + allTools.size());
    }

    private void addTool(ToolType type, int count) {
        for (int i = 0; i < count; i++) {
            Tools tool = new Tools();
            tool.setToolId(toolIdCounter.getAndIncrement());
            tool.setToolType(type);
            tool.setToolStatus(Tools.STATUS_FREE);
            this.allTools.add(tool);
        }
    }

    // 提供给 ResourceManagerThread 和 ToolServiceImpl 调用的方法
    public List<Tools> getAllToolInstances() {
        return allTools;
    }
}