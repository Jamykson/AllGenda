package com.allgenda.exception;

import com.allgenda.dto.ErrorResponseDTO;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(
        RecursoNaoEncontradoException.class
    )
    public ResponseEntity<ErrorResponseDTO>
        handleRecursoNaoEncontrado(
            RecursoNaoEncontradoException ex
        ) {

        ErrorResponseDTO erro =
            new ErrorResponseDTO(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage()
            );

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(erro);
    }

    @ExceptionHandler(
        IllegalArgumentException.class
    )
    public ResponseEntity<ErrorResponseDTO>
        handleIllegalArgument(
            IllegalArgumentException ex
        ) {

        ErrorResponseDTO erro =
            new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage()
            );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(erro);
    }
}