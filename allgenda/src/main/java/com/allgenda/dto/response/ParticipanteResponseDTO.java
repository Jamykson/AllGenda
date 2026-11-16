package com.allgenda.dto.response;

import java.util.UUID;

// Um participante aceito da disciplina. "anotacoes" conta só as anotações do grupo dele nesta disciplina:
// as privadas não entram na conta, para a lista não revelar que existem. O front usa o número para avisar
// o dono, antes de remover o membro, de quantas anotações ficam no grupo.
public record ParticipanteResponseDTO(
    UUID usuarioId,
    String nome,
    String papel, // código do papel: "DONO" ou "MEMBRO"
    long anotacoes
) {}
