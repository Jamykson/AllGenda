package com.allgenda.exception;

// Lançada quando a operação conflita com o estado atual (ex: convite já respondido).
public class ConflitoException extends RuntimeException {
    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
