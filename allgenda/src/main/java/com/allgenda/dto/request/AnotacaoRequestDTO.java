package com.allgenda.dto.request;

import java.util.UUID;
import java.util.List;

public record AnotacaoRequestDTO(
    UUID aulaId,
    UUID autorId, // adição temporária antes da sprint 3.
    String conteudo,
    List<String> tags
) {}

