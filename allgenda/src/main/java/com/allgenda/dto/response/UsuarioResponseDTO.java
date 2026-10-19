package com.allgenda.dto.response;

import java.util.UUID;

// Dados públicos do usuário; senha e hash nunca fazem parte da resposta.
public record UsuarioResponseDTO(
    UUID id,
    String nome,
    String email
) {}
