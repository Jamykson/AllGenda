package com.allgenda.controller;

import com.allgenda.dto.response.ParticipanteResponseDTO;
import com.allgenda.service.ParticipanteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/disciplinas/{disciplinaId}/participantes")
public class ParticipanteController {

    private final ParticipanteService service;

    public ParticipanteController(ParticipanteService service) {
        this.service = service;
    }

    @GetMapping
    public List<ParticipanteResponseDTO> listar(@PathVariable UUID disciplinaId) {
        return service.listar(disciplinaId);
    }

    @DeleteMapping("/{usuarioId}")
    public ResponseEntity<Void> remover(@PathVariable UUID disciplinaId, @PathVariable UUID usuarioId) {
        service.remover(disciplinaId, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
