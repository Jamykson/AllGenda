package com.allgenda.framework.assistente;

import java.util.UUID;
import java.util.function.Consumer;

// Assistente de IA do grupo: responde perguntas usando as anotações e as imagens de todos os encontros
// do grupo. Quando a pergunta vem da página de um encontro, esse encontro fica em destaque.
public interface AssistenteService {

    // valida a conversa e o acesso e monta o pedido ao modelo; todos os erros saem aqui, antes de a
    // resposta começar a ser enviada
    PedidoAoModelo prepararDoGrupo(UUID grupoId, PerguntaAoAssistente pergunta);

    // o mesmo, com o material do grupo do encontro e o encontro em destaque
    PedidoAoModelo prepararDoEncontro(UUID encontroId, PerguntaAoAssistente pergunta);

    // gera a resposta e a entrega em pedaços; bloqueia até o fim
    void responder(PedidoAoModelo pedido, Consumer<String> aoReceberTexto);
}
