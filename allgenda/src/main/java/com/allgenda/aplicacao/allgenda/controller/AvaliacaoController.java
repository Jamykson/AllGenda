package com.allgenda.aplicacao.allgenda.controller;

import com.allgenda.aplicacao.allgenda.dto.request.AvaliacaoRequestDTO;
import com.allgenda.aplicacao.allgenda.dto.response.AvaliacaoResponseDTO;
import com.allgenda.aplicacao.allgenda.mapper.AvaliacaoMapper;
import com.allgenda.framework.extensao.DadosDoEvento;
import com.allgenda.aplicacao.allgenda.model.Avaliacao;
import com.allgenda.framework.service.EventoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/avaliacoes")
public class AvaliacaoController {

    private final EventoService service;
    private final AvaliacaoMapper mapper;

    public AvaliacaoController(EventoService service, AvaliacaoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<AvaliacaoResponseDTO> cadastrar(@RequestBody AvaliacaoRequestDTO dto) {
        String tipo = dto.tipo() == null ? null : dto.tipo().name();
        DadosDoEvento dados = new DadosDoEvento(dto.disciplinaId(), dto.descricao(), dto.data(), tipo);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(service.criar(Avaliacao.TIPO, dados)));
    }

    @GetMapping
    public List<AvaliacaoResponseDTO> listarPorDisciplina(@RequestParam UUID disciplinaId) {
        return mapper.toDtoList(service.listarPorGrupo(disciplinaId));
    }
}
