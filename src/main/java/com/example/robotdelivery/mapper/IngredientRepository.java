package com.example.robotdelivery.mapper;

import com.example.robotdelivery.pojo.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Map;

public interface IngredientRepository extends JpaRepository<Ingredient, Integer> {
    // 修正：返回所有食材的位置和名称（每个条目是一个map，含"key"和"value"）
    @Query("SELECT new map(i.position as key, i.name as value) FROM Ingredient i")
    List<Map<String, Object>> findAllPositionToName(); // 返回List<Map>，而非Map
}