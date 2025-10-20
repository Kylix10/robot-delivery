package com.example.robotdelivery.mapper;

import com.example.robotdelivery.pojo.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DishMapper extends JpaRepository<Dish, Integer> {
}


