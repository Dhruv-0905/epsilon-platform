package com.epsilon.dto.category;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for category response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String categoryName;
    private String description;
    private String colorCode;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
