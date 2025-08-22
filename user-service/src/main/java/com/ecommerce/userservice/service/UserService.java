package com.ecommerce.userservice.service;

import com.ecommerce.userservice.DTO.requestDTO.*;
import com.ecommerce.userservice.DTO.responseDTO.UserRegistrationResponse;
import com.ecommerce.userservice.entity.Role;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.mapper.UserMapper;
import com.ecommerce.userservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {



    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserMapper userMapper){
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }


    public List<UserRegistrationResponse> getAllUsers() {

        List<User> users = userRepository.findAll();
        return userMapper.toRegistrationResponseList(users);

//        return userRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());

    }

    public UserRegistrationResponse getUserById(UUID uuid){
       User user = userRepository.findById(uuid).orElseThrow(() ->new IllegalArgumentException("User not found"));
       return userMapper.toUserRegistrationResponse(user);

//     return userRepository.findById(uuid).map(this::toResponse).orElseThrow(() -> new IllegalArgumentException("User not found"));

    }

    @Transactional
    public UserRegistrationResponse assignRole(UUID id, AssignRoleRequest assignRoleRequest) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.getRoles().add(assignRoleRequest.getRole());
        User savedUser = userRepository.save(user);
        return userMapper.toUserRegistrationResponse(savedUser);
    }

    @Transactional
    public UserRegistrationResponse removeRole(UUID uuid, AssignRoleRequest assignRoleRequest){
        User user = userRepository.findById(uuid).orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.getRoles().remove(assignRoleRequest.getRole());
        User savedUser = userRepository.save(user);
        return userMapper.toUserRegistrationResponse(savedUser);
    }


    public UserRegistrationResponse createCustomer(CustomerRegistrationRequest request) {
        validateUserDoesNotExist(request.getUsername(), request.getEmail());

        User user = userMapper.toCustomer(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(Role.CUSTOMER));

        User savedUser = userRepository.save(user);
        return userMapper.toUserRegistrationResponse(savedUser);
    }

    public UserRegistrationResponse createStore(StoreRegistrationRequest request) {
        validateUserDoesNotExist(request.getUsername(), request.getEmail());

        User user = userMapper.toStore(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(Role.STORE));

        User savedUser = userRepository.save(user);
        return userMapper.toUserRegistrationResponse(savedUser);
    }

    public UserRegistrationResponse createAdmin(AdminRegistrationRequest request) {

        validateUserDoesNotExist(request.getUsername(), request.getEmail());

        User user = userMapper.toAdmin(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(Role.ADMIN));

        User savedUser = userRepository.save(user);
        return userMapper.toUserRegistrationResponse(savedUser);
    }


    public void changePassword(UUID uuid, ChangePasswordRequest changePasswordRequest){
        User user = userRepository.findById(uuid).orElseThrow(() -> new IllegalArgumentException("User not found"));

        if(!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), user.getPassword())){
            throw new IllegalArgumentException("Entered password doesn't match with your actual password");
        }

        user.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        userRepository.save(user);
    }

    public void deleteUser(UUID uuid) {
        if (!userRepository.existsById(uuid)) {
            throw new IllegalArgumentException("User not found");
        }

        userRepository.deleteById(uuid);
    }


    private void validateUserDoesNotExist(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists, choose another one.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists.");
        }
    }


}

