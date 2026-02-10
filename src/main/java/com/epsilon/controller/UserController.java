package com.epsilon.controller;

import com.epsilon.dto.ApiResponse;
import com.epsilon.dto.user.UserRegistrationRequest;
import com.epsilon.dto.user.UserResponse;
import com.epsilon.dto.user.UserUpdateRequest;
import com.epsilon.entity.User;
import com.epsilon.service.UserService;
import com.epsilon.util.EntityMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user operations.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Create a new user account")
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {
        
        log.info("Registering new user: {}", request.getEmail());
        
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(request.getPasswordHash());
        user.setDefaultCurrency(request.getDefaultCurrency());
        
        User savedUser = userService.registerUser(user);
        UserResponse response = EntityMapper.toUserResponse(savedUser);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("User registered successfully", response));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Retrieve user profile by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long userId) {
        log.debug("Fetching user with ID: {}", userId);
        
        User user = userService.getUserById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        UserResponse response = EntityMapper.toUserResponse(user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get user by email", description = "Retrieve user profile by email address")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(@PathVariable String email) {
        log.debug("Fetching user with email: {}", email);
        
        User user = userService.getUserByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        
        UserResponse response = EntityMapper.toUserResponse(user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update user profile", description = "Update user information")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request) {
        
        log.info("Updating user: {}", userId);
        
        User updateData = new User();
        updateData.setFirstName(request.getFirstName());
        updateData.setLastName(request.getLastName());
        updateData.setEmail(request.getEmail());
        updateData.setDefaultCurrency(request.getDefaultCurrency());
        
        User updatedUser = userService.updateUser(userId, updateData);
        UserResponse response = EntityMapper.toUserResponse(updatedUser);
        
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", response));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Deactivate user", description = "Soft-delete user account")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable Long userId) {
        log.info("Deactivating user: {}", userId);
        
        userService.deactivateUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully", null));
    }
}
