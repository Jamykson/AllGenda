package com.allgenda.controller;

import com.allgenda.dto.request.AnotacaoRequestDTO;
import com.allgenda.dto.response.AnotacaoResponseDTO;
import com.allgenda.exception.RequisicaoInvalidaException;
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

    // filtros aceitos: ?aulaId=..., ?disciplinaId=... (opcionalmente com &tag=...) ou ?tag=...
    @GetMapping
    public List<AnotacaoResponseDTO> listar(@RequestParam(required = false) UUID aulaId,
                                            @RequestParam(required = false) UUID disciplinaId,
                                            @RequestParam(required = false) String tag) {
        if (aulaId != null) {
            if (disciplinaId != null || tag != null) {
                throw new RequisicaoInvalidaException("O filtro por aula não pode ser combinado com outros filtros.");
            }
            return service.listarPorAula(aulaId);
        }
        if (disciplinaId != null) {
            return service.listarPorDisciplina(disciplinaId, tag);
        }
        if (tag != null) {
            return service.listarPorTag(tag);
        }
        throw new RequisicaoInvalidaException("Informe um filtro: aulaId, disciplinaId ou tag.");
    }

    @PostMapping("/{id}/tags") // corpo: lista de nomes de tags, ex: ["java", "poo"]
    public AnotacaoResponseDTO associarTags(@PathVariable UUID id, @RequestBody List<String> tags) {
        return service.associarTags(id, tags);
    }

    @PatchMapping("/{id}")
    public AnotacaoResponseDTO editar(@PathVariable UUID id, @RequestBody AnotacaoUpdateDTO dto) {
        return service.editar(id, dto);
    }
}
