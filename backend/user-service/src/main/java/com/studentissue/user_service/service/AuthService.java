package com.studentissue.user_service.service;

import com.studentissue.user_service.dto.AuthResponse;
import com.studentissue.user_service.dto.LoginRequest;
import com.studentissue.user_service.dto.RegisterRequest;
import com.studentissue.user_service.model.Role;
import com.studentissue.user_service.model.User;
import com.studentissue.user_service.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;

import java.util.regex.Pattern;

@Service
public class AuthService {

    // student: nume.prenume@stud.acs.upb.ro, x@stud.etti.upb.ro
    private static final Pattern STUDENT_EMAIL =
            Pattern.compile("^[a-z0-9._%+-]+@stud\\.[a-z]+\\.upb\\.ro$");

    // administrator: nume@upb.ro
    private static final Pattern ADMIN_EMAIL =
            Pattern.compile("^[a-z0-9._%+-]+@upb\\.ro$");


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final boolean adminRegistrationEnabled;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       @Value("${app.admin-registration-enabled}") boolean adminRegistrationEnabled) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.adminRegistrationEnabled = adminRegistrationEnabled;
    }

        public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        Role role;
        if (STUDENT_EMAIL.matcher(email).matches()) {
            role = Role.STUDENT;
        } else if (ADMIN_EMAIL.matcher(email).matches()) {
            if (!adminRegistrationEnabled) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Conturile de administrator nu se pot crea prin înregistrare");
            }
            role = Role.ADMIN;
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Folosește adresa instituțională: @stud.<facultate>.upb.ro (student) sau @upb.ro (administrator)");
        }

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Există deja un cont cu acest email");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .role(role)
                .build();
        user = userRepository.save(user);

        return toResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Email sau parolă incorectă"));

        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), user.getId(),
                user.getEmail(), user.getRole().name());
    }
}