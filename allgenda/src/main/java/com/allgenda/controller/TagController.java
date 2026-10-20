package com.allgenda.controller;

import com.allgenda.dto.request.TagUpdateDTO;
import com.allgenda.dto.response.TagResponseDTO;
import com.allgenda.service.TagService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Tags são da disciplina. Criar uma tag acontece ao atribuí-la a uma anotação (POST /api/anotacoes/{id}/tags).
@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService service;

    public TagController(TagService service) {
        this.service = service;
    }

    // ?disciplinaId=: as tags da disciplina; sem filtro: as de todas as disciplinas do usuário
    @GetMapping
    public List<TagResponseDTO> listar(@RequestParam(required = false) UUID disciplinaId) {
        return service.listar(disciplinaId);
    }

    @PatchMapping("/{id}")
    public TagResponseDTO renomear(@PathVariable UUID id, @RequestBody TagUpdateDTO dto) {
        return service.renomear(id, dto.nome());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID id) {
        service.excluir(id);
    }
}
