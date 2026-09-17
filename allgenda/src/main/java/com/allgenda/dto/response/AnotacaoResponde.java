package com.allgenda.dto.response;

import java.util.UUID;

public record AnotacaoResponse(
    UUID id,
    String conteudo,
    String autorNome
    //List<String> tags
) {}