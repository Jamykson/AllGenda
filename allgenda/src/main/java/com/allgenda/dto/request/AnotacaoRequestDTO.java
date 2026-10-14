package com.allgenda.dto.request;

import java.util.UUID;
import java.util.List;

public record AnotacaoRequestDTO(
    UUID aulaId,
    String conteudo,
    List<String> tags
) {}

