package com.allgenda.exception;

import java.util.UUID;

public class DisciplinaNaoEncontradaException extends RuntimeException {

    public DisciplinaNaoEncontradaException(UUID disciplinaId) {
        super("Disciplina não encontrada: " + disciplinaId);
    }
}