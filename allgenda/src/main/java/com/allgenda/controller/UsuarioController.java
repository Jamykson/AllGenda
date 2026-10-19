package com.allgenda.controller;

import com.allgenda.dto.request.UsuarioRequestDTO;
import com.allgenda.dto.response.UsuarioResponseDTO;
import com.allgenda.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponseDTO cadastrar(@RequestBody UsuarioRequestDTO dto) {
        return service.cadastrar(dto);
    }
}
