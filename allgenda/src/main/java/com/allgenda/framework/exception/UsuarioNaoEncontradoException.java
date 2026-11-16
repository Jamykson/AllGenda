package com.allgenda.framework.exception;

import java.util.UUID;

public class UsuarioNaoEncontradoException extends RecursoNaoEncontradoException {
    public UsuarioNaoEncontradoException(UUID id) {
        super("Usuário não encontrado: " + id);
    }

    public UsuarioNaoEncontradoException(String email) {
        super("Usuário não encontrado: " + email);
    }
}
