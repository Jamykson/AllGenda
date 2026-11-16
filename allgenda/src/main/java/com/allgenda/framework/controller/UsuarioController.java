package com.allgenda.framework.controller;

import com.allgenda.framework.dto.request.UsuarioRequestDTO;
import com.allgenda.framework.dto.response.UsuarioResponseDTO;
import com.allgenda.framework.service.UsuarioService;
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
