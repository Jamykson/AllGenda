package com.allgenda.exception;

// Lançada quando o usuário passa do limite de perguntas ao assistente por hora. Vira 429 no GlobalExceptionHandler.
public class LimiteDePerguntasException extends RuntimeException {
    public LimiteDePerguntasException(String mensagem) {
        super(mensagem);
    }
}
