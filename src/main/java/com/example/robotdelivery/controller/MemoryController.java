package com.example.robotdelivery.controller;

import com.example.robotdelivery.pojo.Dish;
import com.example.robotdelivery.pojo.vo.MemoryVO;
import com.example.robotdelivery.pojo.vo.WorkstationVo;
import com.example.robotdelivery.service.IOrderService;
import com.example.robotdelivery.service.MemoryManager;
import com.example.robotdelivery.service.MemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    private final MemoryService memoryService;
    @Autowired
    private MemoryManager memoryManager;

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

//    @PostMapping("/allocate")
//    public ResponseEntity<Boolean> testAllocate(@RequestParam int dishId, @RequestParam int size) {
//        Dish testDish = new Dish();
//        testDish.setDishId(dishId);
//        testDish.setRequiredSpace(size);
//        return ResponseEntity.ok(memoryManager.allocateForOrder(testDish));
//    }
//
//    @PostMapping("/release")
//    public ResponseEntity<Boolean> testRelease(@RequestParam int dishId) {
//        return ResponseEntity.ok(memoryManager.releaseOrderPartition(dishId));
//    }

}