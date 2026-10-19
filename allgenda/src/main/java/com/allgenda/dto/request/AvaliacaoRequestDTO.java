package com.allgenda.dto.request;

import com.allgenda.model.TipoAvaliacao;
import java.time.LocalDate;
import java.util.UUID;

public record AvaliacaoRequestDTO(
    UUID disciplinaId,
    String descricao,
    LocalDate data,
    TipoAvaliacao tipo
) {}
