package com.restaurant_management.restaurant_management_backend.menu.categories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurant_management.restaurant_management_backend.menu.categories.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {}
