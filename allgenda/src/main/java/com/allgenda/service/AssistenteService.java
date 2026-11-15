package com.allgenda.service;

import com.allgenda.dto.request.PerguntaAssistenteDTO;

import java.util.UUID;
import java.util.function.Consumer;

// Assistente de IA da disciplina: responde perguntas usando as anotações e as imagens de todas as aulas
// da disciplina. Na página de uma aula, essa aula fica em destaque.
public interface AssistenteService {

    // valida a conversa e o acesso e monta o pedido ao modelo; todos os erros saem aqui, antes de a
    // resposta começar a ser enviada. O material é o da disciplina da aula, com a aula em destaque.
    PedidoAoModelo prepararDaAula(UUID aulaId, PerguntaAssistenteDTO pergunta);

    // o mesmo, a partir da disciplina, sem aula em destaque
    PedidoAoModelo prepararDaDisciplina(UUID disciplinaId, PerguntaAssistenteDTO pergunta);

    // gera a resposta e a entrega em pedaços; bloqueia até o fim
    void responder(PedidoAoModelo pedido, Consumer<String> aoReceberTexto);
}
