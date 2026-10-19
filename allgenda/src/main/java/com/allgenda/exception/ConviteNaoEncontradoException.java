package com.allgenda.exception;

import java.util.UUID;

public class ConviteNaoEncontradoException extends RecursoNaoEncontradoException {
    public ConviteNaoEncontradoException(UUID id) {
        super("Convite não encontrado: " + id);
    }
}
