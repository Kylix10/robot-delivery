package com.example.robotdelivery.mapper;

import com.example.robotdelivery.pojo.RobotInitFlag;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RobotInitFlagRepository extends JpaRepository<RobotInitFlag, Integer> {

    // 加悲观锁查询唯一记录（singleRow=1），避免并发修改
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM RobotInitFlag f WHERE f.singleRow = 1")
    Optional<RobotInitFlag> findUniqueFlagWithLock();
}