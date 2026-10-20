package com.allgenda.service;

import com.allgenda.dto.request.PerguntaAssistenteDTO;

import java.util.UUID;
import java.util.function.Consumer;

// Assistente de IA da página da aula: responde perguntas usando as anotações e as imagens da aula.
public interface AssistenteService {

    // valida a conversa e o acesso e monta o pedido ao modelo; todos os erros saem aqui, antes de a
    // resposta começar a ser enviada
    PedidoAoModelo preparar(UUID aulaId, PerguntaAssistenteDTO pergunta);

    // gera a resposta e a entrega em pedaços; bloqueia até o fim
    void responder(PedidoAoModelo pedido, Consumer<String> aoReceberTexto);
}
