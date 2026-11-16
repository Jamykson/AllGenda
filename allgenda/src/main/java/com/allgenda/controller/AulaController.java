package com.allgenda.controller;

import com.allgenda.dto.request.AulaRequestDTO;
import com.allgenda.dto.request.AulaUpdateDTO;
import com.allgenda.dto.response.AulaResponseDTO;
import com.allgenda.mapper.AulaMapper;
import com.allgenda.framework.extensao.DadosDoEncontro;
import com.allgenda.model.Aula;
import com.allgenda.service.AtualizacaoDeEncontro;
import com.allgenda.service.EncontroService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/aulas")
public class AulaController {

    private final EncontroService service;
    private final AulaMapper mapper;

    public AulaController(EncontroService service, AulaMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<AulaResponseDTO> cadastrar(@RequestBody AulaRequestDTO dto) {
        AulaResponseDTO criada = mapper.toDto(service.criar(Aula.TIPO,
                new DadosDoEncontro(dto.disciplinaId(), dto.data(), dto.horario(), dto.topico())));
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @GetMapping
    public List<AulaResponseDTO> listarPorDisciplina(@RequestParam UUID disciplinaId) {
        return mapper.toDtoList(service.listarPorGrupo(disciplinaId));
    }

    @GetMapping("/{id}")
    public AulaResponseDTO buscarPorId(@PathVariable UUID id) {
        return mapper.toDto(service.buscarPorId(id));
    }

    @PatchMapping("/{id}")
    public AulaResponseDTO atualizar(@PathVariable UUID id, @RequestBody AulaUpdateDTO dto) {
        AtualizacaoDeEncontro atualizacao = dto == null ? null
                : new AtualizacaoDeEncontro(dto.data(), dto.horario(), dto.topico(), dto.cancelada());
        return mapper.toDto(service.atualizar(id, atualizacao));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
