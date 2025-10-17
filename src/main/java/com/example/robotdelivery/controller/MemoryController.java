package com.example.robotdelivery.controller;

import com.example.robotdelivery.pojo.vo.MemoryVO;
import com.example.robotdelivery.service.MemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    private final MemoryService memoryService;

    @Autowired
    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /**
     * GET /api/memory/status
     * 获取当前工作台内存分区状态 (仅保留此接口)
     * @return MemoryVO 包含总览信息和分区列表
     */
    @GetMapping("/status")
    public ResponseEntity<MemoryVO> getMemoryStatus() {
        MemoryVO status = memoryService.getMemoryStatus();
        return ResponseEntity.ok(status);
    }

}