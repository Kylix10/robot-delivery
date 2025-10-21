package com.example.robotdelivery.mapper;

import com.example.robotdelivery.pojo.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DishRepository extends JpaRepository<Dish, Integer> {

    Dish findByDishName(String dishName);
}

