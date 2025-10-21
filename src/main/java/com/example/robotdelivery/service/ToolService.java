package com.example.robotdelivery.service;

import com.example.robotdelivery.pojo.Tools;
import org.springframework.stereotype.Service;

import java.util.List;


public interface ToolService {
    // 获取当前系统中所有工具的列表
    List<Tools> getAllTools();

    // ... 其他工具管理方法
}