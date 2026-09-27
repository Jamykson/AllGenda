package com.allgenda.controller;

import com.allgenda.dto.request.AnotacaoRequestDTO;
import com.allgenda.dto.response.AnotacaoResponseDTO;
import com.allgenda.service.AnotacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/anotacoes")
public class AnotacaoController {

    private final AnotacaoService service;

    public AnotacaoController(AnotacaoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AnotacaoResponseDTO> cadastrar(@RequestBody AnotacaoRequestDTO dto) {
        AnotacaoResponseDTO criada = service.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @PostMapping("/{id}/tags") // corpo: lista de nomes de tags, ex: ["java", "poo"]
    public AnotacaoResponseDTO associarTags(@PathVariable UUID id, @RequestBody List<String> tags) {
        return service.associarTags(id, tags);
    }
}
