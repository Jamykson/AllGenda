package com.allgenda.exception;

import java.util.UUID;

public abstract class RecursoNaoEncontradoException extends RuntimeException {
    protected RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}

/*/ Essa é uma classe abstrata e genérica que é pai das exceções mais específicas, de maneira que
    cada service cria sua própria exceção específica, mas o ControllerAdvice só precisa de um ExceptionHandler) /*/