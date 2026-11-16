package com.allgenda.aplicacao.allgenda.dto.request;

import java.time.LocalDate;
import java.util.Set;

import com.allgenda.framework.model.DiaSemana;
import com.allgenda.framework.model.FrequenciaRecorrencia;

// horaInicio e horaFim no formato HH:mm; frequencia ausente vale SEMANAL.
public record RecorrenciaRequestDTO(
    Set<DiaSemana> diasSemana,
    String horaInicio,
    String horaFim,
    LocalDate dataInicio,
    LocalDate dataFim,
    FrequenciaRecorrencia frequencia
    ) {}
