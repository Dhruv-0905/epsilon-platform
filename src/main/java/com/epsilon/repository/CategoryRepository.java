package com.epsilon.repository;

import com.epsilon.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Category entity.
 * Categories are user-specific (each user has their own custom categories).
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    /**
     * Find all categories for a specific user.
     * 
     * Generated SQL: SELECT * FROM categories WHERE user_id = ?
     */
    List<Category> findByUserId(Long userId);
    
    /**
     * Find active categories for a user.
     * 
     * Used for dropdown lists when creating transactions.
     */
    List<Category> findByUserIdAndIsActiveTrue(Long userId);
    
    /**
     * Find a category by name for a specific user.
     * 
     * Used to prevent duplicate category names per user.
     * Different users CAN have categories with the same name.
     */
    Optional<Category> findByUserIdAndCategoryName(Long userId, String categoryName);
    
    /**
     * Check if a category name already exists for a user.
     */
    boolean existsByUserIdAndCategoryName(Long userId, String categoryName);
}
