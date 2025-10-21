package com.example.robotdelivery.controller;

import com.example.robotdelivery.pojo.vo.MemoryVO;
import com.example.robotdelivery.pojo.vo.WorkstationVo;
import com.example.robotdelivery.service.MemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    /**
     * GET /api/memory/workstations
     * 获取工作台（分区）的详细表格状态 (用于工作台表格)
     * @return List<WorkstationVo> 包含表格所需的详细信息
     */
    @GetMapping("/workstations")
    public ResponseEntity<List<WorkstationVo>> getWorkstationStatus() {
        List<WorkstationVo> statusList = memoryService.getWorkstationDetails();
        return ResponseEntity.ok(statusList);
    }

}