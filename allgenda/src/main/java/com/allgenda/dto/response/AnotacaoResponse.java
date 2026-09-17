package com.allgenda.dto.response;

import java.util.UUID;

public record AnotacaoResponseDTO(
    UUID id,
    String conteudo,
    String autorNome
    //List<String> tags
) {}