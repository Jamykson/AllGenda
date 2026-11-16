package com.allgenda.framework.assistente;

import java.util.List;

// O que o assistente envia ao modelo de linguagem: as instruções e a conversa, com o material da
// grupo em blocos de texto e de imagem na primeira mensagem. Não depende do SDK do fornecedor.
public record PedidoAoModelo(String instrucoes, List<Mensagem> mensagens) {

    public enum Papel { USUARIO, ASSISTENTE }

    public record Mensagem(Papel papel, List<Bloco> blocos) {}

    public sealed interface Bloco permits Texto, Imagem {}

    // fimDoMaterial marca o último bloco do material do grupo: tudo até ele se repete a cada pergunta
    // da conversa e pode ficar em cache no fornecedor
    public record Texto(String texto, boolean fimDoMaterial) implements Bloco {
        public Texto(String texto) {
            this(texto, false);
        }
    }

    public record Imagem(byte[] conteudo, String contentType) implements Bloco {}
}
