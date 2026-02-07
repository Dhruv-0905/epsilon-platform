package com.epsilon.repository;

import com.epsilon.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{

    /**
     * Find a user by email address.
     * 
     * Spring Data JPA automatically generates the SQL query based on the method name:
     * Method name: findByEmail
     * Generated SQL: SELECT * FROM users WHERE email = ?
     * 
     * @param email The email to search for
     * @return Optional containing the user if found, empty Optional if not found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user exists with the given email.
     * 
     * Generated SQL: SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END FROM users WHERE email = ?
     * 
     * @param email The email to check
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Find all active users.
     * 
     * Generated SQL: SELECT * FROM users WHERE is_active = TRUE
     * 
     * @return List of active users
     */
    java.util.List<User> findByIsActiveTrue();
}
