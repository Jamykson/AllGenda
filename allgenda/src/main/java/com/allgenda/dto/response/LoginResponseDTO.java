package com.allgenda.dto.response;

// Token e dados públicos do usuário; expiraEm é informado em segundos.
public record LoginResponseDTO(
    String token,
    String tipo,
    long expiraEm,
    UsuarioResponseDTO usuario
) {}
