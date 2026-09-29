package com.allgenda.dto;

public record ErrorResponseDTO(
    int status,
    String mensagem
) {
}