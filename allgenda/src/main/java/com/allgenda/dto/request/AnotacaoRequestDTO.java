package com.allgenda.dto.request;

import com.allgenda.model.VisibilidadeAnotacao;

import java.util.UUID;
import java.util.List;

public record AnotacaoRequestDTO(
    UUID aulaId,
    String titulo, // opcional, mas é preciso informar o título ou o conteúdo
    String conteudo,
    List<String> tags,
    VisibilidadeAnotacao visibilidade // opcional: ausente = GRUPO
) {}

