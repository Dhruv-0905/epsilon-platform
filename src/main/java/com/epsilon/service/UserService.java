package com.epsilon.service;

import com.epsilon.entity.User;
import com.epsilon.enums.Currency;
import com.epsilon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for User operations.
 * Handles business logic for user management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Register a new user.
     *
     * Business Rules:
     * 1. Email must be unique
     * 2. Password length validated at entity level
     * 3. Default currency is set to USD if not provided
     */
    @Transactional
    public User registerUser(User user) {
        log.info("Attempting to register user with email: {}", user.getEmail());

        if (userRepository.existsByEmail(user.getEmail())) {
            log.error("Registration failed: Email {} already exists", user.getEmail());
            throw new IllegalArgumentException("Email already registered: " + user.getEmail());
        }

        if (user.getDefaultCurrency() == null) {
            user.setDefaultCurrency(Currency.USD);
            log.debug("No currency provided, defaulting to USD");
        }

        if (user.getIsActive() == null) {
            user.setIsActive(true);
        }

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());
        return savedUser;
    }

    public Optional<User> getUserById(Long userId) {
        log.debug("Fetching user with ID: {}", userId);
        return userRepository.findById(userId);
    }

    public Optional<User> getUserByEmail(String email) {
        log.debug("Fetching user with email: {}", email);
        return userRepository.findByEmail(email);
    }

    public List<User> getActiveUsers() {
        log.debug("Fetching all active users");
        return userRepository.findByIsActiveTrue();
    }

    /**
     * Update user profile.
     *
     * Business Rules:
     * 1. User must exist
     * 2. Cannot change email to one already used by another user
     */
    @Transactional
    public User updateUser(Long userId, User updatedUser) {
        log.info("Attempting to update user with ID: {}", userId);

        User existingUser = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (!existingUser.getEmail().equals(updatedUser.getEmail())) {
            if (userRepository.existsByEmail(updatedUser.getEmail())) {
                throw new IllegalArgumentException("Email already in use: " + updatedUser.getEmail());
            }
            existingUser.setEmail(updatedUser.getEmail());
        }

        existingUser.setFirstName(updatedUser.getFirstName());
        existingUser.setLastName(updatedUser.getLastName());
        existingUser.setDefaultCurrency(updatedUser.getDefaultCurrency());

        User saved = userRepository.save(existingUser);
        log.info("User updated successfully: {}", userId);
        return saved;
    }

    /**
     * Soft-deactivate a user.
     * Keeps history intact.
     */
    @Transactional
    public void deactivateUser(Long userId) {
        log.info("Deactivating user with ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        user.setIsActive(false);
        userRepository.save(user);

        log.info("User deactivated successfully: {}", userId);
    }

    public boolean isUserActive(Long userId) {
        return userRepository.findById(userId)
            .map(User::getIsActive)
            .orElse(false);
    }
}
