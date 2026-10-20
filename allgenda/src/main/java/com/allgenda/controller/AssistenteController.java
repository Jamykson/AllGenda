package com.allgenda.controller;

import com.allgenda.dto.request.PerguntaAssistenteDTO;
import com.allgenda.service.AssistenteService;
import com.allgenda.service.FalhaDoModeloException;
import com.allgenda.service.PedidoAoModelo;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// Assistente de IA da aula. A resposta chega por Server-Sent Events, em pedaços, à medida que o
// modelo a gera. Os erros de validação e de acesso saem antes do stream, como ProblemDetail.
@RestController
@RequestMapping("/api/aulas/{aulaId}/assistente")
public class AssistenteController {

    private static final Logger log = LoggerFactory.getLogger(AssistenteController.class);
    private static final Duration TEMPO_MAXIMO = Duration.ofMinutes(3);

    private final AssistenteService service;
    // a geração passa a maior parte do tempo esperando a API, então cada uma ganha uma thread virtual
    private final ExecutorService geracoes = Executors.newVirtualThreadPerTaskExecutor();

    public AssistenteController(AssistenteService service) {
        this.service = service;
    }

    // eventos: "texto" ({"texto": "..."}) várias vezes, depois "fim" ({}); ou "erro" ({"mensagem": "..."})
    // se o modelo falhar no meio do caminho
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter perguntar(@PathVariable UUID aulaId, @RequestBody PerguntaAssistenteDTO pergunta) {
        PedidoAoModelo pedido = service.preparar(aulaId, pergunta);

        SseEmitter emissor = new SseEmitter(TEMPO_MAXIMO.toMillis());
        Future<?> geracao = geracoes.submit(() -> gerar(pedido, emissor));
        emissor.onTimeout(() -> geracao.cancel(true));
        return emissor;
    }

    private void gerar(PedidoAoModelo pedido, SseEmitter emissor) {
        try {
            service.responder(pedido, texto -> enviar(emissor, "texto", Map.of("texto", texto)));
            enviar(emissor, "fim", Map.of());
            emissor.complete();
        }
        catch (UncheckedIOException e) {
            // o cliente fechou a conexão (ex: o aluno clicou em "Parar"); não há a quem avisar
            log.debug("O cliente do assistente desconectou: {}", e.getMessage());
            emissor.complete();
        }
        catch (FalhaDoModeloException e) {
            avisarErro(emissor, e.getMessage());
        }
        catch (RuntimeException e) {
            log.error("Erro inesperado ao gerar a resposta do assistente.", e);
            avisarErro(emissor, "Não foi possível gerar a resposta. Tente de novo.");
        }
    }

    private static void avisarErro(SseEmitter emissor, String mensagem) {
        try {
            enviar(emissor, "erro", Map.of("mensagem", mensagem));
            emissor.complete();
        }
        catch (UncheckedIOException e) {
            emissor.complete();
        }
    }

    private static void enviar(SseEmitter emissor, String evento, Object dados) {
        try {
            emissor.send(SseEmitter.event().name(evento).data(dados, MediaType.APPLICATION_JSON));
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @PreDestroy
    void encerrar() {
        geracoes.shutdownNow();
    }
}
