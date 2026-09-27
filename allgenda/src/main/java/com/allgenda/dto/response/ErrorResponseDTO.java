package com.allgenda.dto.response;

import java.time.LocalDateTime;

public record ErrorResponseDTO(
    LocalDateTime timestamp,
    int status,
    String mensagem
) {}