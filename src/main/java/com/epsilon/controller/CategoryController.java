package com.epsilon.controller;

import com.epsilon.dto.ApiResponse;
import com.epsilon.dto.category.CategoryRequest;
import com.epsilon.dto.category.CategoryResponse;
import com.epsilon.entity.Category;
import com.epsilon.service.CategoryService;
import com.epsilon.util.EntityMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for category operations.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Categories", description = "Category management endpoints")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping("/user/{userId}")
    @Operation(summary = "Create category", description = "Create a new category for a user")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @PathVariable Long userId,
            @Valid @RequestBody CategoryRequest request) {
        
        log.info("Creating category '{}' for user: {}", request.getCategoryName(), userId);
        
        Category category = new Category();
        category.setCategoryName(request.getCategoryName());
        category.setDescription(request.getDescription());
        category.setColorCode(request.getColorCode());
        
        Category savedCategory = categoryService.createCategory(userId, category);
        CategoryResponse response = EntityMapper.toCategoryResponse(savedCategory);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("Category created successfully", response));
    }

    @GetMapping("/{categoryId}")
    @Operation(summary = "Get category by ID", description = "Retrieve category details")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long categoryId) {
        log.debug("Fetching category: {}", categoryId);
        
        Category category = categoryService.getCategoryById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));
        
        CategoryResponse response = EntityMapper.toCategoryResponse(category);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user categories", description = "Retrieve all categories for a user")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getUserCategories(@PathVariable Long userId) {
        log.debug("Fetching categories for user: {}", userId);
        
        List<CategoryResponse> categories = categoryService.getCategoriesByUserId(userId)
            .stream()
            .map(EntityMapper::toCategoryResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/user/{userId}/active")
    @Operation(summary = "Get active categories", description = "Retrieve active categories for a user")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getActiveCategories(@PathVariable Long userId) {
        log.debug("Fetching active categories for user: {}", userId);
        
        List<CategoryResponse> categories = categoryService.getActiveCategoriesByUserId(userId)
            .stream()
            .map(EntityMapper::toCategoryResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @PutMapping("/{categoryId}")
    @Operation(summary = "Update category", description = "Update category information")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryRequest request) {
        
        log.info("Updating category: {}", categoryId);
        
        Category updateData = new Category();
        updateData.setCategoryName(request.getCategoryName());
        updateData.setDescription(request.getDescription());
        updateData.setColorCode(request.getColorCode());
        
        Category updatedCategory = categoryService.updateCategory(categoryId, updateData);
        CategoryResponse response = EntityMapper.toCategoryResponse(updatedCategory);
        
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", response));
    }

    @DeleteMapping("/{categoryId}")
    @Operation(summary = "Deactivate category", description = "Soft-delete a category")
    public ResponseEntity<ApiResponse<Void>> deactivateCategory(@PathVariable Long categoryId) {
        log.info("Deactivating category: {}", categoryId);
        
        categoryService.deactivateCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Category deactivated successfully", null));
    }
}
