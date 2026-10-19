package com.allgenda.dto.request;

import java.util.UUID;
import java.util.List;

public record AnotacaoRequestDTO(
    UUID aulaId,
    String titulo, // opcional, mas é preciso informar o título ou o conteúdo
    String conteudo,
    List<String> tags
) {}

