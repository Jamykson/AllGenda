package com.allgenda.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.allgenda.dto.request.RecorrenciaRequestDTO;
import com.allgenda.dto.response.RecorrenciaResponseDTO;
import com.allgenda.service.RecorrenciaService;

@RestController
@RequestMapping("/api")
public class RecorrenciaController {

    private final RecorrenciaService service;

    public RecorrenciaController(RecorrenciaService service) {
        this.service = service;
    }

    // as recorrências de todas as disciplinas da pessoa, para a agenda e a pesquisa
    @GetMapping("/recorrencias")
    public List<RecorrenciaResponseDTO> listar() {
        return service.listarDoUsuario();
    }

    @GetMapping("/disciplinas/{disciplinaId}/recorrencia")
    public RecorrenciaResponseDTO buscar(@PathVariable UUID disciplinaId) {
        return service.buscar(disciplinaId);
    }

    @PutMapping("/disciplinas/{disciplinaId}/recorrencia")
    public RecorrenciaResponseDTO salvar(@PathVariable UUID disciplinaId, @RequestBody RecorrenciaRequestDTO dto) {
        return service.salvar(disciplinaId, dto);
    }

    @DeleteMapping("/disciplinas/{disciplinaId}/recorrencia")
    public ResponseEntity<Void> remover(@PathVariable UUID disciplinaId) {
        service.remover(disciplinaId);
        return ResponseEntity.noContent().build();
    }
}
