package com.allgenda.exception;

import java.util.UUID;

public class EncontroNaoEncontradoException extends RecursoNaoEncontradoException {
    public EncontroNaoEncontradoException(UUID id) {
        super("Encontro não encontrado: " + id);
    }
}
