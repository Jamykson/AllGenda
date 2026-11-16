package com.allgenda.aplicacao.allgenda.controller;

import com.allgenda.aplicacao.allgenda.dto.request.PerguntaAssistenteDTO;
import com.allgenda.framework.assistente.PerguntaAoAssistente;
import com.allgenda.framework.assistente.PerguntaAoAssistente.Foco;
import com.allgenda.framework.assistente.PerguntaAoAssistente.Mensagem;
import com.allgenda.framework.assistente.PerguntaAoAssistente.Remetente;
import com.allgenda.framework.assistente.PerguntaAoAssistente.TipoFoco;
import com.allgenda.framework.assistente.AssistenteService;
import com.allgenda.framework.assistente.FalhaDoModeloException;
import com.allgenda.framework.assistente.PedidoAoModelo;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// Assistente de IA da disciplina, com duas portas: a da disciplina e a de uma aula (que a põe em
// destaque). A resposta chega por Server-Sent Events, em pedaços, à medida que o modelo a gera. Os
// erros de validação e de acesso saem antes do stream, como ProblemDetail.
@RestController
@RequestMapping("/api")
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
    @PostMapping(path = "/disciplinas/{disciplinaId}/assistente", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter perguntarSobreADisciplina(@PathVariable UUID disciplinaId, @RequestBody PerguntaAssistenteDTO pergunta) {
        return iniciar(service.prepararDoGrupo(disciplinaId, paraONucleo(pergunta)));
    }

    // mesmos eventos, com o material da disciplina da aula e a aula em destaque
    @PostMapping(path = "/aulas/{aulaId}/assistente", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter perguntarSobreAAula(@PathVariable UUID aulaId, @RequestBody PerguntaAssistenteDTO pergunta) {
        return iniciar(service.prepararDoEncontro(aulaId, paraONucleo(pergunta)));
    }

    // o JSON do AllGenda (papel, foco) vira a pergunta neutra do núcleo; valores ausentes continuam ausentes,
    // para o núcleo responder 400 como antes
    private static PerguntaAoAssistente paraONucleo(PerguntaAssistenteDTO dto) {
        if (dto == null || dto.mensagens() == null) {
            return new PerguntaAoAssistente(null);
        }
        List<Mensagem> mensagens = dto.mensagens().stream()
                .map(m -> m == null ? null : new Mensagem(
                        m.papel() == null ? null : Remetente.valueOf(m.papel().name()),
                        m.texto(),
                        m.foco() == null ? null : new Foco(
                                m.foco().tipo() == null ? null : TipoFoco.valueOf(m.foco().tipo().name()),
                                m.foco().id())))
                .toList();
        return new PerguntaAoAssistente(mensagens);
    }

    private SseEmitter iniciar(PedidoAoModelo pedido) {
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
