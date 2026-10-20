package com.allgenda.dto.response;

import java.util.UUID;

public record TagResponseDTO(
    UUID id,
    String nome,
    UUID disciplinaId // cada disciplina tem as suas tags
) {}
