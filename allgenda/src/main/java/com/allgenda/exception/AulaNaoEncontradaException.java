package com.allgenda.exception;

import java.util.UUID;

public class AulaNaoEncontradaException extends RecursoNaoEncontradoException {
    public AulaNaoEncontradaException(UUID id) {
        super("Aula nao encontrada: " + id);
    }
}