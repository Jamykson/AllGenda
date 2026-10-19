package com.allgenda.controller;

import com.allgenda.dto.response.AnotacaoResponseDTO;
import com.allgenda.service.AnotacaoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/disciplinas/{disciplinaId}/anotacoes")
public class AnotacaoColegaController {

    private final AnotacaoService service;

    public AnotacaoColegaController(AnotacaoService service) {
        this.service = service;
    }

    @GetMapping("/colegas")
    public List<AnotacaoResponseDTO> listar(@PathVariable UUID disciplinaId) {
        return service.listarDosColegas(disciplinaId);
    }
}