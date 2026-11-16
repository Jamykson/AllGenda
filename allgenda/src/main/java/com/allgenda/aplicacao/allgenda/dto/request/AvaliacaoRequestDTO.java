package com.allgenda.aplicacao.allgenda.dto.request;

import com.allgenda.aplicacao.allgenda.model.TipoAvaliacao;
import java.time.LocalDate;
import java.util.UUID;

public record AvaliacaoRequestDTO(
    UUID disciplinaId,
    String descricao,
    LocalDate data,
    TipoAvaliacao tipo
) {}
