package com.allgenda.dto.request;

import java.time.LocalDate;

// Edição parcial: o que vier nulo não muda. "cancelada" marca ou desmarca o cancelamento da aula.
public record AulaUpdateDTO(
    LocalDate data,
    String horario,
    String topico,
    Boolean cancelada
    ) {}
