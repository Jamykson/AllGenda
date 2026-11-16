package com.allgenda.controller;

import com.allgenda.dto.request.AvaliacaoRequestDTO;
import com.allgenda.dto.response.AvaliacaoResponseDTO;
import com.allgenda.mapper.AvaliacaoMapper;
import com.allgenda.service.AvaliacaoService;
import com.allgenda.service.DadosDoEvento;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/avaliacoes")
public class AvaliacaoController {

    private final AvaliacaoService service;
    private final AvaliacaoMapper mapper;

    public AvaliacaoController(AvaliacaoService service, AvaliacaoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<AvaliacaoResponseDTO> cadastrar(@RequestBody AvaliacaoRequestDTO dto) {
        String tipo = dto.tipo() == null ? null : dto.tipo().name();
        DadosDoEvento dados = new DadosDoEvento(dto.disciplinaId(), dto.descricao(), dto.data(), tipo);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(service.cadastrar(dados)));
    }

    @GetMapping
    public List<AvaliacaoResponseDTO> listarPorDisciplina(@RequestParam UUID disciplinaId) {
        return mapper.toDtoList(service.listarPorDisciplina(disciplinaId));
    }
}
