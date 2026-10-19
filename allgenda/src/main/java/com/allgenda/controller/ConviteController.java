package com.allgenda.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.allgenda.dto.request.ConviteRequestDTO;
import com.allgenda.dto.response.ConviteResponseDTO;
import com.allgenda.model.StatusParticipacao;
import com.allgenda.service.ConviteService;

@RestController
@RequestMapping("/api") // os caminhos de convite vivem em dois prefixos (/disciplinas/... e /convites/...)
public class ConviteController {

    private final ConviteService service;

    public ConviteController(ConviteService service) {
        this.service = service;
    }

    @PostMapping("/disciplinas/{disciplinaId}/convites")
    public ResponseEntity<ConviteResponseDTO> enviar(@PathVariable UUID disciplinaId, @RequestBody ConviteRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.enviar(disciplinaId, dto));
    }

    @GetMapping("/convites")
    public List<ConviteResponseDTO> listar(@RequestParam(defaultValue = "PENDENTE") StatusParticipacao status) {
        return service.listar(status);
    }

    @PatchMapping("/convites/{id}/aceitar")
    public ConviteResponseDTO aceitar(@PathVariable UUID id) {
        return service.aceitar(id);
    }

    @PatchMapping("/convites/{id}/recusar")
    public ConviteResponseDTO recusar(@PathVariable UUID id) {
        return service.recusar(id);
    }
}