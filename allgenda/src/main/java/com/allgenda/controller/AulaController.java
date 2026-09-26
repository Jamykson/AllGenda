package com.allgenda.controller;

import com.allgenda.dto.request.AulaRequestDTO;
import com.allgenda.dto.response.AulaResponseDTO;
import com.allgenda.service.AulaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/aulas")
public class AulaController {

    private final AulaService service;

    public AulaController(AulaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AulaResponseDTO> cadastrar(@RequestBody AulaRequestDTO dto) {
        AulaResponseDTO criada = service.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @GetMapping
    public List<AulaResponseDTO> listarPorDisciplina(@RequestParam UUID disciplinaId) {
        return service.listarPorDisciplina(disciplinaId);
    }

    @GetMapping("/{id}")
    public AulaResponseDTO buscarPorId(@PathVariable UUID id) {
        return service.buscarPorId(id);
    }
}