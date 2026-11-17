package com.studentissue.user_service.controller;

import com.studentissue.user_service.dto.UserResponse;
import com.studentissue.user_service.model.User;
import com.studentissue.user_service.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // X-User-Id este pus de API Gateway după validarea JWT-ului
    @GetMapping("/me")
    public UserResponse me(@RequestHeader("X-User-Id") Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilizator inexistent"));
        return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(),
                user.getLastName(), user.getRole().name());
    }
}