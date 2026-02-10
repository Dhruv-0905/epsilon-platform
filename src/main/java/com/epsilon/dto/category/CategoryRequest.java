package com.epsilon.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for category creation/update.
 */
@Data
public class CategoryRequest {
    
    @NotBlank(message = "Category name is required")
    @Size(max = 50, message = "Category name cannot exceed 50 characters")
    private String categoryName;
    
    private String description;
    
    @Size(max = 7, message = "Color code must be in hex format (e.g., #FF5733)")
    private String colorCode;
}
