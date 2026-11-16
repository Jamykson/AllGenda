package com.allgenda.framework.exception;

import java.util.UUID;

public class ParticipanteNaoEncontradoException extends RecursoNaoEncontradoException {
    public ParticipanteNaoEncontradoException(UUID usuarioId) {
        super("Participante não encontrado: " + usuarioId);
    }
}
