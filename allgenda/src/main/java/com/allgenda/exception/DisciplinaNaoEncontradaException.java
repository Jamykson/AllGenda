package com.allgenda.exception;

import java.util.UUID;

public class DisciplinaNaoEncontradaException
    extends RecursoNaoEncontradoException {

    public DisciplinaNaoEncontradaException(
        UUID id
    ) {
        super(
            "Disciplina não encontrada: " + id
        );
    }

    public DisciplinaNaoEncontradaException(
        String mensagem
    ) {
        super(mensagem);
    }
}