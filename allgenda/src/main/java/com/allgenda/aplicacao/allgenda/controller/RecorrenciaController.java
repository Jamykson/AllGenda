package com.allgenda.aplicacao.allgenda.controller;

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

import com.allgenda.aplicacao.allgenda.dto.request.RecorrenciaRequestDTO;
import com.allgenda.aplicacao.allgenda.dto.response.RecorrenciaResponseDTO;
import com.allgenda.aplicacao.allgenda.mapper.RecorrenciaMapper;
import com.allgenda.framework.service.DadosDaRecorrencia;
import com.allgenda.framework.service.RecorrenciaService;

@RestController
@RequestMapping("/api")
public class RecorrenciaController {

    private final RecorrenciaService service;
    private final RecorrenciaMapper mapper;

    public RecorrenciaController(RecorrenciaService service, RecorrenciaMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    // as recorrências de todas as disciplinas da pessoa, para a agenda e a pesquisa
    @GetMapping("/recorrencias")
    public List<RecorrenciaResponseDTO> listar() {
        return mapper.toDtoList(service.listarDoUsuario());
    }

    @GetMapping("/disciplinas/{disciplinaId}/recorrencia")
    public RecorrenciaResponseDTO buscar(@PathVariable UUID disciplinaId) {
        return mapper.toDto(service.buscar(disciplinaId));
    }

    @PutMapping("/disciplinas/{disciplinaId}/recorrencia")
    public RecorrenciaResponseDTO salvar(@PathVariable UUID disciplinaId, @RequestBody RecorrenciaRequestDTO dto) {
        DadosDaRecorrencia dados = dto == null ? null : new DadosDaRecorrencia(dto.diasSemana(), dto.horaInicio(),
                dto.horaFim(), dto.dataInicio(), dto.dataFim(), dto.frequencia());
        return mapper.toDto(service.salvar(disciplinaId, dados));
    }

    @DeleteMapping("/disciplinas/{disciplinaId}/recorrencia")
    public ResponseEntity<Void> remover(@PathVariable UUID disciplinaId) {
        service.remover(disciplinaId);
        return ResponseEntity.noContent().build();
    }
}
