package com.allgenda.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice // intercepta exceções lançadas por qualquer controller e as converte em respostas HTTP
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class) // cobre todas as subclasses (Aula, Disciplina, Anotacao, Usuario...)
    public ProblemDetail tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ProblemDetail tratarRequisicaoInvalida(RequisicaoInvalidaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class) // cobre exceções padrão do spring por formato de requisição inválido.
    public ProblemDetail tratarMensagemIlegivel(HttpMessageNotReadableException ex) {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, 
            "Formato de requisição inválido. Verifique os tipos e formatos dos campos enviados (ex: data no padrão AAAA-MM-DD)."
        );
    }
}
