package com.allgenda.service;

import java.time.LocalDate;

// Edição parcial de um encontro: o que vier nulo não muda. "cancelada" marca ou desmarca o cancelamento.
public record AtualizacaoDeEncontro(LocalDate data, String horario, String topico, Boolean cancelada) {}
