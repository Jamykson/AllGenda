package com.allgenda.dto.request;

import java.util.UUID;

public record AnotacaoRequestDTO(
    UUID aulaId,
    String conteudo
    // List<String> tags
) {}

