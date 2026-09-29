package com.allgenda.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record HorarioRecorrenteResponseDTO(
    List<Integer> dias,
    LocalTime horaInicio,
    LocalTime horaFim,
    LocalDate dataInicio,
    LocalDate dataFim
) {
}