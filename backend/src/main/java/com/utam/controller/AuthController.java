package com.utam.controller;

import com.utam.common.ApiResponse;
import com.utam.model.User;
import com.utam.model.dto.LoginRequest;
import com.utam.model.dto.LoginResponse;
import com.utam.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPassword().equals(request.getPassword())) {
                String token = "mock-token-" + user.getId();
                return ApiResponse
                        .success(new LoginResponse(token, user.getUsername(), user.getRole(), user.getTenantCode()));
            }
        }
        // In a real app, use 401 Unauthorized
        return ApiResponse.error("Invalid credentials");
    }
}
