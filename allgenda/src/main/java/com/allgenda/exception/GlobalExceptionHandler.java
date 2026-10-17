package com.allgenda.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice // intercepta exceções lançadas por qualquer controller e as converte em respostas HTTP
public class GlobalExceptionHandler {

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ProblemDetail tratarStatus(org.springframework.web.server.ResponseStatusException ex) {
        return ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ProblemDetail tratarConflito(org.springframework.dao.DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Dados em conflito com um registro existente.");
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class) // cobre todas as subclasses (Aula, Disciplina, Anotacao, Usuario...)
    public ProblemDetail tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ProblemDetail tratarRequisicaoInvalida(RequisicaoInvalidaException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ProblemDetail tratarAcessoNegado(AcessoNegadoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ArquivoMuitoGrandeException.class)
    public ProblemDetail tratarArquivoMuitoGrande(ArquivoMuitoGrandeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE, ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class) // arquivo barrado pelo limite de multipart do servidor, antes de chegar ao service
    public ProblemDetail tratarUploadGrandeDemais(MaxUploadSizeExceededException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE, "O arquivo excede o tamanho máximo permitido.");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail tratarArquivoAusente(MissingServletRequestPartException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Envie o arquivo no campo '" + ex.getRequestPartName() + "'.");
    }

    @ExceptionHandler(MultipartException.class)
    public ProblemDetail tratarMultipartInvalido(MultipartException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Requisição multipart inválida.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class) // cobre exceções padrão do spring por formato de requisição inválido.
    public ProblemDetail tratarMensagemIlegivel(HttpMessageNotReadableException ex) {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, 
            "Formato de requisição inválido. Verifique os tipos e formatos dos campos enviados (ex: data no padrão AAAA-MM-DD)."
        );
    }
}
