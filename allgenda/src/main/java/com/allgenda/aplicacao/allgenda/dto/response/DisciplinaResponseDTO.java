package com.allgenda.aplicacao.allgenda.dto.response;

import java.util.UUID;

public record DisciplinaResponseDTO(
    UUID id,
    String nome,
    String descricao
) {}