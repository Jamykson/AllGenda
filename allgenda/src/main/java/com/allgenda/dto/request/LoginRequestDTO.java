package com.allgenda.dto.request;

// Credenciais de login; o service valida o e-mail antes de conferir o hash da senha.
public record LoginRequestDTO(
    String email,
    String senha
) {}
