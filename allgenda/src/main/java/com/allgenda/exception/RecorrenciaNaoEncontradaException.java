package com.allgenda.exception;

import java.util.UUID;

public class RecorrenciaNaoEncontradaException extends RecursoNaoEncontradoException {
    public RecorrenciaNaoEncontradaException(UUID disciplinaId) {
        super("A disciplina não tem recorrência: " + disciplinaId);
    }
}
