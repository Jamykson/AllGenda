package com.allgenda.dto.response;
public record LoginResponseDTO(String token, String tipo, long expiraEm, UsuarioResponseDTO usuario) {}
