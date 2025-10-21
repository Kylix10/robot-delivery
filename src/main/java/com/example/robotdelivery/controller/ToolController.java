package com.example.robotdelivery.controller;

import com.example.robotdelivery.pojo.Tools;
import com.example.robotdelivery.service.ToolService;
import com.example.robotdelivery.pojo.vo.ToolVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tools")
public class ToolController {

    @Autowired
    private ToolService toolService; // 假设的工具服务，用于获取工具列表

    /**
     * 获取所有烹饪器具的状态列表
     * 前端访问路径: /api/tools/status
     * 返回结构: List<ToolVo>
     */
    @GetMapping("/status")
    public ResponseEntity<List<ToolVo>> getAllToolStatus() {
        // 1. 从 Service 层获取所有工具的实体列表
        // 假设 ToolService.getAllTools() 返回 List<Tools>
        List<Tools> allTools = toolService.getAllTools();

        // 2. 将 Tools 实体列表转换为 ToolVo 列表
        List<ToolVo> toolVos = allTools.stream()
                .map(ToolVo::fromTools) // 使用 ToolVo 中的静态工厂方法进行转换
                .collect(Collectors.toList());

        // 3. 返回响应
        return ResponseEntity.ok(toolVos);
    }
}