package com.allgenda.exception;

import java.util.UUID;
import com.allgenda.exception.RecursoNaoEncontradoException;

public class DisciplinaNaoEncontradaException extends RecursoNaoEncontradoException {

    public DisciplinaNaoEncontradaException(UUID disciplinaId) {
        super("Disciplina não encontrada: " + disciplinaId);
    }
}