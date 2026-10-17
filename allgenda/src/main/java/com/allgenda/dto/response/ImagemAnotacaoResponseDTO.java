package com.allgenda.dto.response;

import java.util.UUID;

// url: caminho do GET que devolve a imagem (exige o token, como os demais endpoints)
public record ImagemAnotacaoResponseDTO(
    UUID id,
    String nome,
    String contentType,
    long tamanho,
    String url
) {}
