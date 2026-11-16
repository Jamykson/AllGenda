package com.allgenda.framework.controller;

import com.allgenda.framework.dto.request.LoginRequestDTO;
import com.allgenda.framework.dto.response.LoginResponseDTO;
import com.allgenda.framework.service.AuthService;
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
