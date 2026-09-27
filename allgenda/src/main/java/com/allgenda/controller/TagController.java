package com.allgenda.controller;

import com.allgenda.dto.response.TagResponseDTO;
import com.allgenda.service.TagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService service;

    public TagController(TagService service) {
        this.service = service;
    }

    @GetMapping // usado pela tela de anotação para sugerir tags já existentes
    public List<TagResponseDTO> listarTodas() {
        return service.listarTodas();
    }
}
