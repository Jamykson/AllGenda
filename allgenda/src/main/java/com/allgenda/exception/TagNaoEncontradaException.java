package com.allgenda.exception;

import java.util.UUID;

public class TagNaoEncontradaException extends RecursoNaoEncontradoException {
    public TagNaoEncontradaException(UUID id) {
        super("Tag não encontrada: " + id);
    }
}
