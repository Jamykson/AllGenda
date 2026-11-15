package com.allgenda.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.allgenda.model.DiaSemana;
import com.allgenda.model.FrequenciaRecorrencia;

public record RecorrenciaResponseDTO(
    UUID disciplinaId,
    List<DiaSemana> diasSemana,
    String horaInicio,
    String horaFim,
    LocalDate dataInicio,
    LocalDate dataFim,
    FrequenciaRecorrencia frequencia
    ) {}
