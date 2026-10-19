package com.allgenda.dto.request;

// Dados de cadastro; os campos obrigatórios e os limites são validados no UsuarioService.
public record UsuarioRequestDTO(
    String nome,
    String email,
    String senha
) {}
