package com.allgenda.dto.request;

import java.util.List;
import java.util.UUID;

// A conversa com o assistente da aula, inteira: o servidor não guarda o histórico, então o cliente
// reenvia as mensagens anteriores a cada pergunta. Começa e termina com uma mensagem do usuário.
public record PerguntaAssistenteDTO(List<Mensagem> mensagens) {

    public enum Papel { USUARIO, ASSISTENTE }

    public enum TipoFoco { ANOTACAO, IMAGEM }

    // foco: a anotação ou a imagem da aula sobre a qual o usuário pergunta (opcional)
    public record Mensagem(Papel papel, String texto, Foco foco) {}

    public record Foco(TipoFoco tipo, UUID id) {}
}
