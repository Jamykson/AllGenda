package com.allgenda.exception;

// Lançada quando o usuário autenticado não tem permissão sobre o recurso (ex: não é o autor). Vira 403 no GlobalExceptionHandler.
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
