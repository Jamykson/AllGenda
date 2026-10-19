package com.allgenda.controller;

import com.allgenda.dto.request.LoginRequestDTO;
import com.allgenda.dto.response.LoginResponseDTO;
import com.allgenda.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody LoginRequestDTO dto) {
        return service.login(dto);
    }
}
