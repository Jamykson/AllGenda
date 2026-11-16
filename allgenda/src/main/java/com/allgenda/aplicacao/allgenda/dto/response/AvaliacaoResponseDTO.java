package com.allgenda.aplicacao.allgenda.dto.response;

import com.allgenda.aplicacao.allgenda.model.TipoAvaliacao;
import java.time.LocalDate;
import java.util.UUID;

public record AvaliacaoResponseDTO(
    UUID id,
    String descricao,
    LocalDate data,
    TipoAvaliacao tipo,
    UUID disciplinaId,
    String disciplinaNome
) {}
