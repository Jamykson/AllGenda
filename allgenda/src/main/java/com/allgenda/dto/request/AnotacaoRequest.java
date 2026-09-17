package com.allgenda.dto.request;

import java.util.UUID;

public record AnotacaoReponseDTO(
    UUID aulaId,
    String conteudo
    // List<String> tags
) {}

