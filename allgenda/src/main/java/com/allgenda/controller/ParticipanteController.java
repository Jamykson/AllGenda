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
        // o JSON mostra a pessoa, o papel e quantas anotações do grupo ela escreveu
        return service.listar(disciplinaId).stream()
                .map(participante -> new ParticipanteResponseDTO(
                        participante.participacao().getUsuario().getId(),
                        participante.participacao().getUsuario().getNome(),
                        participante.participacao().getPapel(),
                        participante.anotacoesDoGrupo()))
                .toList();
    }

    @DeleteMapping("/{usuarioId}")
    public ResponseEntity<Void> remover(@PathVariable UUID disciplinaId, @PathVariable UUID usuarioId) {
        service.remover(disciplinaId, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
