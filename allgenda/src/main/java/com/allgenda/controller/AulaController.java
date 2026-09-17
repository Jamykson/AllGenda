package com.allgenda.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.allgenda.dto.AulaRequest;
import com.allgenda.dto.AulaResponse;
import com.allgenda.exception.DisciplinaNaoEncontradaException;
import com.allgenda.model.Aula;
import com.allgenda.repository.AulaRepository;
import com.allgenda.service.AulaService;

@RestController
@RequestMapping("/disciplinas/{disciplinaId}/aulas")
public class AulaController {

    @Autowired
    private AulaService aulaService;

    @Autowired
    private AulaRepository aulaRepository;

    @PostMapping
    public ResponseEntity<AulaResponse> criar(@PathVariable UUID disciplinaId, @RequestBody AulaRequest request) {
        Aula aula = aulaService.criar(request.data(), request.horario(), request.topico(), disciplinaId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(aula));
    }

    @GetMapping
    public List<AulaResponse> listar(@PathVariable UUID disciplinaId) {
        return aulaRepository.findByDisciplinaIdOrderByDataAsc(disciplinaId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @ExceptionHandler(DisciplinaNaoEncontradaException.class)
    public ResponseEntity<Void> handleDisciplinaNaoEncontrada() {
        return ResponseEntity.notFound().build();
    }

    private AulaResponse toResponse(Aula aula) {
        return new AulaResponse(aula.getId(), aula.getData(), aula.getHorario(), aula.getTopico());
    }
}