package com.allgenda.exception;

// Lançada quando os dados enviados pelo cliente são inválidos (ex: campo obrigatório vazio). Vira 400 no GlobalExceptionHandler.
public class RequisicaoInvalidaException extends RuntimeException {
    public RequisicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
