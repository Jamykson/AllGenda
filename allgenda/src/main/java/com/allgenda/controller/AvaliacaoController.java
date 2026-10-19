package com.allgenda.controller;

import com.allgenda.dto.request.AvaliacaoRequestDTO;
import com.allgenda.dto.response.AvaliacaoResponseDTO;
import com.allgenda.service.AvaliacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/avaliacoes")
public class AvaliacaoController {

    private final AvaliacaoService service;

    public AvaliacaoController(AvaliacaoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AvaliacaoResponseDTO> cadastrar(@RequestBody AvaliacaoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.cadastrar(dto));
    }

    @GetMapping
    public List<AvaliacaoResponseDTO> listarPorDisciplina(@RequestParam UUID disciplinaId) {
        return service.listarPorDisciplina(disciplinaId);
    }
}
