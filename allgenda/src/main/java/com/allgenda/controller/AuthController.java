package com.allgenda.controller;
import com.allgenda.dto.request.*;
import com.allgenda.dto.response.*;
import com.allgenda.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }
    @PostMapping("/auth/login")
    public LoginResponseDTO login(@RequestBody LoginRequestDTO dto) { return service.login(dto); }
    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponseDTO cadastrar(@RequestBody UsuarioRequestDTO dto) { return service.cadastrar(dto); }
}
