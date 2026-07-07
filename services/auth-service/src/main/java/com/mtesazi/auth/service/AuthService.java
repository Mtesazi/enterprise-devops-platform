package com.mtesazi.auth.service;

import com.mtesazi.auth.dto.AuthResponse;
import com.mtesazi.auth.dto.LoginRequest;
import com.mtesazi.auth.dto.RefreshTokenRequest;
import com.mtesazi.auth.dto.RegisterRequest;
import com.mtesazi.auth.dto.UserMeResponse;
import com.mtesazi.auth.entity.User;
import com.mtesazi.auth.exception.InvalidCredentialsException;
import com.mtesazi.auth.exception.UserAlreadyExistsException;
import com.mtesazi.auth.exception.UserDisabledException;
import com.mtesazi.auth.repository.UserRepository;
import com.mtesazi.auth.security.JwtProperties;
import com.mtesazi.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final String ROLE_USER = "ROLE_USER";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.setRoles(resolveRoles(request.getRoles()));

        User savedUser = userRepository.save(user);
        return buildAuthResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!user.isEnabled()) {
            throw new UserDisabledException("User account is disabled");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        String username = jwtTokenProvider.validateRefreshTokenAndGetUsername(request.getRefreshToken());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("User not found for refresh token"));

        if (!user.isEnabled()) {
            throw new UserDisabledException("User account is disabled");
        }

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserMeResponse me(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Authenticated user not found"));

        return new UserMeResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled(),
                user.getRoles(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        return new AuthResponse(
                accessToken,
                refreshToken,
                TOKEN_TYPE,
                jwtProperties.getAccessTokenExpiration(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles()
        );
    }

    private List<String> resolveRoles(List<String> requestedRoles) {
        if (requestedRoles == null || requestedRoles.isEmpty()) {
            return List.of(ROLE_USER);
        }
        return requestedRoles.stream().map(String::trim).filter(role -> !role.isBlank()).toList();
    }
}
