package com.allgenda.controller;

import com.allgenda.dto.DisciplinaRequestDTO;
import com.allgenda.dto.DisciplinaResponseDTO;
import com.allgenda.dto.HorarioRecorrenteRequestDTO;
import com.allgenda.service.DisciplinaService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/disciplinas")
public class DisciplinaController {

    private final DisciplinaService service;

    public DisciplinaController(
        DisciplinaService service
    ) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<
        List<DisciplinaResponseDTO>
    > listar() {

        return ResponseEntity.ok(
            service.listar()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<
        DisciplinaResponseDTO
    > buscarPorId(
        @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
            service.buscarPorId(id)
        );
    }

    @PostMapping
    public ResponseEntity<
        DisciplinaResponseDTO
    > criar(
        @RequestBody
        DisciplinaRequestDTO request
    ) {

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                service.criar(request)
            );
    }

    @PutMapping("/{id}/horario")
    public ResponseEntity<
        DisciplinaResponseDTO
    > salvarHorario(
        @PathVariable UUID id,

        @RequestBody
        HorarioRecorrenteRequestDTO request
    ) {

        return ResponseEntity.ok(
            service.salvarHorario(
                id,
                request
            )
        );
    }

    @DeleteMapping("/{id}/horario")
    public ResponseEntity<
        DisciplinaResponseDTO
    > removerHorario(
        @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
            service.removerHorario(id)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
        @PathVariable UUID id
    ) {

        service.excluir(id);

        return ResponseEntity
            .noContent()
            .build();
    }
}