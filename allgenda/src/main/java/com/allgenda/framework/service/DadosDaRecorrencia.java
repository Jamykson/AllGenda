package com.allgenda.framework.service;

import com.allgenda.framework.model.DiaSemana;
import com.allgenda.framework.model.FrequenciaRecorrencia;

import java.time.LocalDate;
import java.util.Set;

// Quando os encontros do grupo se repetem. horaInicio e horaFim no formato HH:mm; frequencia ausente vale SEMANAL.
public record DadosDaRecorrencia(Set<DiaSemana> diasSemana, String horaInicio, String horaFim,
                                 LocalDate dataInicio, LocalDate dataFim, FrequenciaRecorrencia frequencia) {}
