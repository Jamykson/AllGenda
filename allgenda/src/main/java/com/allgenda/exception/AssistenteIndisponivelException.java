package com.allgenda.exception;

// Lançada quando o assistente da aula não pode atender (ex: sem chave da API configurada). Vira 503 no GlobalExceptionHandler.
public class AssistenteIndisponivelException extends RuntimeException {
    public AssistenteIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
