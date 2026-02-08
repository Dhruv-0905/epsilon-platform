package com.epsilon.service;

import com.epsilon.entity.Category;
import com.epsilon.entity.User;
import com.epsilon.repository.CategoryRepository;
import com.epsilon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for Category operations.
 * Categories are scoped per user.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    /**
     * Create a new category for a user.
     *
     * Rules:
     * 1. User must exist
     * 2. Name must be unique per user
     */
    @Transactional
    public Category createCategory(Long userId, Category category) {
        log.info("Creating category '{}' for user ID: {}",
                 category.getCategoryName(), userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (categoryRepository.existsByUserIdAndCategoryName(userId, category.getCategoryName())) {
            throw new IllegalArgumentException(
                "Category '" + category.getCategoryName() + "' already exists for this user"
            );
        }

        category.setUser(user);

        if (category.getIsActive() == null) {
            category.setIsActive(true);
        }

        Category saved = categoryRepository.save(category);
        log.info("Category created successfully with ID: {}", saved.getId());
        return saved;
    }

    public List<Category> getCategoriesByUserId(Long userId) {
        log.debug("Fetching categories for user ID: {}", userId);
        return categoryRepository.findByUserId(userId);
    }

    public List<Category> getActiveCategoriesByUserId(Long userId) {
        log.debug("Fetching active categories for user ID: {}", userId);
        return categoryRepository.findByUserIdAndIsActiveTrue(userId);
    }

    public Optional<Category> getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId);
    }

    @Transactional
    public Category updateCategory(Long categoryId, Category updatedCategory) {
        log.info("Updating category with ID: {}", categoryId);

        Category existing = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));

        if (!existing.getCategoryName().equals(updatedCategory.getCategoryName())) {
            if (categoryRepository.existsByUserIdAndCategoryName(
                    existing.getUser().getId(), updatedCategory.getCategoryName())) {
                throw new IllegalArgumentException(
                    "Category name '" + updatedCategory.getCategoryName() + "' already exists"
                );
            }
            existing.setCategoryName(updatedCategory.getCategoryName());
        }

        existing.setDescription(updatedCategory.getDescription());
        existing.setColorCode(updatedCategory.getColorCode());

        return categoryRepository.save(existing);
    }

    @Transactional
    public void deactivateCategory(Long categoryId) {
        log.info("Deactivating category with ID: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));

        category.setIsActive(false);
        categoryRepository.save(category);

        log.info("Category deactivated successfully: {}", categoryId);
    }

    public boolean verifyCategoryOwnership(Long categoryId, Long userId) {
        return categoryRepository.findById(categoryId)
            .map(category -> category.getUser().getId().equals(userId))
            .orElse(false);
    }
}
