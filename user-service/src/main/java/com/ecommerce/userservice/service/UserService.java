package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.response.ChangePasswordResponse;
import com.ecommerce.userservice.dto.response.UserProfileResponse;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.exception.DuplicateUserException;
import com.ecommerce.userservice.exception.UserNotFoundException;
import com.ecommerce.userservice.mapper.PasswordMapper;
import com.ecommerce.userservice.mapper.UserMapper;
import com.ecommerce.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordMapper passwordMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public User getUserById(UUID userId) {
        log.debug("Fetching user by ID: {}", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", userId);
                    return new UserNotFoundException("User not found with id: " + userId);
                });
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        log.debug("Fetching user by username: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found with username: {}", username);
                    return new UserNotFoundException("User not found with username: " + username);
                });
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new UserNotFoundException("User not found with email: " + email);
                });
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "user:profile", key = "#userId")
    public UserProfileResponse getUserProfile(UUID userId) {
        log.debug("Fetching user profile for ID: {}", userId);
        User user = getUserById(userId);
        return userMapper.toUserProfileResponse(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfileByUsername(String username) {
        log.debug("Fetching user profile for username: {}", username);
        User user = getUserByUsername(username);
        return userMapper.toUserProfileResponse(user);
    }

    @Transactional
    public User saveUser(User user) {
        log.debug("Saving user: {}", user.getUsername());
        User saved = userRepository.save(user);
        log.info("User saved successfully: {} (ID: {})", saved.getUsername(), saved.getUserId());
        return saved;
    }

    @Transactional
    @CachePut(value = "user:profile", key = "#userId")
    public UserProfileResponse updateUserProfile(UUID userId, String firstName, String lastName, String email) {
        log.info("Updating profile for user ID: {}", userId);

        User user = getUserById(userId);

        if (email != null && !user.getEmail().equals(email)) {
            if (existsByEmail(email)) {
                log.warn("Update failed - email already exists: {}", email);
                throw new DuplicateUserException("Email already exists: " + email);
            }
            user.setEmail(email);
        }

        if (firstName != null) {
            user.setFirstName(firstName);
        }
        if (lastName != null) {
            user.setLastName(lastName);
        }

        User updated = userRepository.save(user);
        log.info("User profile updated successfully: {} (ID: {})", updated.getUsername(), updated.getUserId());

        return userMapper.toUserProfileResponse(updated);
    }

    @Transactional
    @CacheEvict(value = "user:profile", key = "#userId")
    public ChangePasswordResponse changePassword(UUID userId, String currentPassword, String newPassword) {
        log.info("Password change request for user ID: {}", userId);

        User user = getUserById(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            log.warn("Password change failed - incorrect current password for user: {}", user.getUsername());
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        refreshTokenService.revokeAllUserTokensByUserId(userId);

        log.info("Password changed successfully for user: {} (ID: {})", user.getUsername(), user.getUserId());

        return passwordMapper.toChangePasswordResponse();
    }

    @Transactional
    @CacheEvict(value = "user:profile", key = "#userId")
    public void lockUser(UUID userId, String adminUsername) {
        log.warn("Locking user account: {}", userId);

        User user = getUserById(userId);
        user.setLocked(true);
        userRepository.save(user);

        refreshTokenService.revokeAllUserTokensByUserId(userId);

        log.warn("User account locked: {} (ID: {}) by admin: {}", user.getUsername(), userId, adminUsername);
    }

    @Transactional
    @CacheEvict(value = "user:profile", key = "#userId")
    public void unlockUser(UUID userId, String adminUsername) {
        log.info("Unlocking user account: {}", userId);

        User user = getUserById(userId);
        user.setLocked(false);
        userRepository.save(user);


        log.info("User account unlocked: {} (ID: {}) by admin: {}", user.getUsername(), userId, adminUsername);
    }

    @Transactional
    @CacheEvict(value = "user:profile", key = "#userId")
    public void disableUser(UUID userId, String adminUsername) {
        log.warn("Disabling user account: {}", userId);

        User user = getUserById(userId);
        user.setEnabled(false);
        userRepository.save(user);

        refreshTokenService.revokeAllUserTokensByUserId(userId);

        log.warn("User account disabled: {} (ID: {}) by admin: {}", user.getUsername(), userId, adminUsername);
    }

    @Transactional
    @CacheEvict(value = "user:profile", key = "#userId")
    public void enableUser(UUID userId, String adminUsername) {
        log.info("Enabling user account: {}", userId);

        User user = getUserById(userId);
        user.setEnabled(true);
        userRepository.save(user);

        log.info("User account enabled: {} (ID: {}) by admin: {}", user.getUsername(), userId, adminUsername);
    }

    @Transactional(readOnly = true)
    public boolean isUserActiveAndUnlocked(UUID userId) {
        return userRepository.existsByUserIdAndEnabledTrueAndLockedFalse(userId);
    }

    @Transactional(readOnly = true)
    public boolean verifyPassword(String username, String rawPassword) {
        User user = getUserByUsername(username);
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }
}

