package com.allgenda.framework.exception;

// Lançada quando o assistente não pode atender (ex: sem chave da API configurada). Vira 503 no GlobalExceptionHandler.
public class AssistenteIndisponivelException extends RuntimeException {
    public AssistenteIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
