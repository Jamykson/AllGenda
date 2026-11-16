package com.allgenda.exception;

import java.util.UUID;

public class RecorrenciaNaoEncontradaException extends RecursoNaoEncontradoException {
    public RecorrenciaNaoEncontradaException(UUID grupoId) {
        super("O grupo não tem recorrência: " + grupoId);
    }
}
