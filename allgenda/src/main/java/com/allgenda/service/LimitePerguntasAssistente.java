package com.allgenda.service;

import com.allgenda.exception.LimiteDePerguntasException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Limita quantas perguntas cada usuário faz ao assistente por hora, para conter o custo da API.
// Fica em memória: reiniciar o back-end zera a contagem, o que basta para uma instância só.
@Component
public class LimitePerguntasAssistente {

    private static final Duration JANELA = Duration.ofHours(1);

    private final int perguntasPorHora;
    private final Map<UUID, Deque<Instant>> perguntasPorUsuario = new ConcurrentHashMap<>();

    public LimitePerguntasAssistente(@Value("${app.assistente.perguntas-por-hora}") int perguntasPorHora) {
        this.perguntasPorHora = perguntasPorHora;
    }

    // conta a pergunta, ou lança 429 se o usuário já fez o máximo na última hora
    public void registrar(UUID usuarioId) {
        Instant agora = Instant.now();
        Deque<Instant> perguntas = perguntasPorUsuario.computeIfAbsent(usuarioId, id -> new ArrayDeque<>());
        synchronized (perguntas) {
            while (!perguntas.isEmpty() && perguntas.peekFirst().isBefore(agora.minus(JANELA))) {
                perguntas.pollFirst();
            }
            if (perguntas.size() >= perguntasPorHora) {
                long minutos = Math.max(1, Duration.between(agora, perguntas.peekFirst().plus(JANELA)).toMinutes());
                throw new LimiteDePerguntasException("Você atingiu o limite de " + perguntasPorHora
                        + " perguntas por hora ao assistente. Tente de novo em " + minutos + " min.");
            }
            perguntas.addLast(agora);
        }
    }
}
