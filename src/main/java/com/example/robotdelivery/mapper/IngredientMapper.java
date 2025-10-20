package com.example.robotdelivery.mapper;

import com.example.robotdelivery.pojo.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IngredientMapper extends JpaRepository<Ingredient, Integer> {
}


