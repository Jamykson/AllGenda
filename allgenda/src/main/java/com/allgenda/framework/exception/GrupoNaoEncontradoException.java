package com.allgenda.framework.exception;

import java.util.UUID;

public class GrupoNaoEncontradoException extends RecursoNaoEncontradoException {

    public GrupoNaoEncontradoException(UUID grupoId) {
        super("Grupo não encontrado: " + grupoId);
    }
}
