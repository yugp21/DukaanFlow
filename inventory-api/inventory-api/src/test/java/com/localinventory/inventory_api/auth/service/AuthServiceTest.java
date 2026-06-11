package com.localinventory.inventory_api.auth.service;

import com.localinventory.inventory_api.auth.dto.RegisterRequest;
import com.localinventory.inventory_api.shop.repository.ShopRepository;
import com.localinventory.inventory_api.user.entity.Role;
import com.localinventory.inventory_api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ShopRepository shopRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@gmail.com");
        request.setMobile("9876543210");
        request.setRole(Role.OWNER);

        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register(request));

        assertTrue(ex.getMessage().contains("Email already registered"));
    }

    @Test
    void shouldThrowWhenMobileAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@gmail.com");
        request.setMobile("9876543210");
        request.setRole(Role.OWNER);

        when(userRepository.existsByEmail("new@gmail.com")).thenReturn(false);
        when(shopRepository.existsByMobile("9876543210")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register(request));

        assertTrue(ex.getMessage().contains("Mobile already registered"));
    }
}