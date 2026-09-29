package com.allgenda.dto;

import java.util.UUID;

public record DisciplinaResponseDTO(
    UUID id,
    String nome,
    HorarioRecorrenteResponseDTO horario
) {
}