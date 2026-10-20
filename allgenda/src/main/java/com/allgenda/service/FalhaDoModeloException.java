package com.allgenda.service;

// Falha do modelo de linguagem depois que a resposta já começou a ser enviada (serviço fora do ar,
// sobrecarregado, erro de rede). A mensagem é mostrada ao usuário.
public class FalhaDoModeloException extends RuntimeException {
    public FalhaDoModeloException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
