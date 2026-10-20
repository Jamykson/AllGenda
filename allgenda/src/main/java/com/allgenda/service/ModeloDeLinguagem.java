package com.allgenda.service;

import java.util.function.Consumer;

// O modelo de linguagem que gera as respostas do assistente. A implementação real chama a API da
// Anthropic; os testes usam uma falsa.
public interface ModeloDeLinguagem {

    // falso quando falta a configuração (ex: a chave da API), e o assistente fica indisponível
    boolean configurado();

    // gera a resposta e entrega o texto em pedaços, à medida que chega; bloqueia até o fim.
    // Lança FalhaDoModeloException se o fornecedor falhar. Se aoReceberTexto lançar uma exceção
    // (ex: o cliente desconectou), a geração é interrompida e a exceção é repassada.
    void responder(PedidoAoModelo pedido, Consumer<String> aoReceberTexto);
}
