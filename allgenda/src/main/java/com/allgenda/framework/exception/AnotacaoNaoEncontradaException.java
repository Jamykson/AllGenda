package com.allgenda.framework.exception;

import java.util.UUID;

public class AnotacaoNaoEncontradaException extends RecursoNaoEncontradoException {
    public AnotacaoNaoEncontradaException(UUID id) {
        super("Anotação não encontrada: " + id);
    }
}
