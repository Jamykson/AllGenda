package com.allgenda.exception;

import java.util.UUID;

public class ImagemNaoEncontradaException extends RecursoNaoEncontradoException {
    public ImagemNaoEncontradaException(UUID id) {
        super("Imagem não encontrada: " + id);
    }
}
