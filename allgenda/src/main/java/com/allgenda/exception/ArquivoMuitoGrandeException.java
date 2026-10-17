package com.allgenda.exception;

import org.springframework.util.unit.DataSize;

// Lançada quando um arquivo enviado passa do limite configurado. Vira 413 no GlobalExceptionHandler.
public class ArquivoMuitoGrandeException extends RuntimeException {
    public ArquivoMuitoGrandeException(DataSize limite) {
        super("O arquivo excede o tamanho máximo de " + limite.toMegabytes() + " MB.");
    }
}
