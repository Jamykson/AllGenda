package com.allgenda.aplicacao.allgenda.dto.response;

import java.util.UUID;

import com.allgenda.framework.model.StatusParticipacao;

public record ConviteResponseDTO(
    UUID id,
    UUID disciplinaId,
    String disciplinaNome,
    UUID usuarioId,
    String usuarioNome,
    String papel, // código do papel: "MEMBRO" para convites
    StatusParticipacao status
) {}
