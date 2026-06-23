package com.epsilon.service;

import com.epsilon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security's bridge between a username string and the full UserDetails object.
 *
 * Called in TWO places:
 *   1. By AuthenticationManager during POST /api/auth/login — to load the user
 *      so BCryptPasswordEncoder can compare the submitted password against the hash.
 *   2. By JwtAuthenticationFilter on every protected request — to load the user
 *      so the JWT subject can be matched and the SecurityContext can be populated.
 *
 * Why a separate class instead of implementing UserDetailsService on User.java?
 *   UserDetailsService is an infrastructure concern (DB lookup). Putting it on
 *   the entity would mix persistence and security responsibilities. Separation
 *   also makes this independently testable with a mocked UserRepository.
 *
 * The class name is prefixed "Epsilon" to avoid ambiguity if Spring Security's
 * own InMemoryUserDetailsManager ever appears on the classpath in tests.
 */
@Service
@RequiredArgsConstructor
public class EpsilonUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user by email address.
     *
     * @param username Spring Security passes the principal identifier here;
     *                 in Epsilon that is always the user's email address.
     * @throws UsernameNotFoundException if no user exists with that email.
     *         Spring Security catches this and converts it to a 401 response.
     *         The message is intentionally generic to prevent user enumeration.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Authentication failed."
                ));
    }
}