package com.allgenda.framework.assistente;

import java.util.List;
import java.util.UUID;

// A conversa com o assistente, inteira: o servidor não guarda o histórico, então o cliente reenvia as
// mensagens anteriores a cada pergunta. Começa e termina com uma mensagem do usuário, alternando.
public record PerguntaAoAssistente(List<Mensagem> mensagens) {

    public enum Remetente { USUARIO, ASSISTENTE }

    public enum TipoFoco { ANOTACAO, IMAGEM }

    // foco: a anotação ou a imagem do grupo sobre a qual o usuário pergunta (opcional)
    public record Mensagem(Remetente remetente, String texto, Foco foco) {}

    public record Foco(TipoFoco tipo, UUID id) {}
}
