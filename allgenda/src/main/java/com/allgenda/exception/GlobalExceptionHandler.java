package com.allgenda.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoExceptin.class)
    public ResponseEntity<ErrorResponseDTO> handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {

        ErrorResponseDTO erro = new ErrorResponseDTO(LocalDateTime.now(),HttpStatus.NOT_FOUND.value(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenerico(Exception ex) {

        ErrorResponseDTO erro = new ErrorResponseDTO(LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erro interno no servidor.");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }
}
