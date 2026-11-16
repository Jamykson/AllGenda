package com.allgenda.framework.acesso;

import java.util.Optional;

import com.allgenda.framework.model.Participacao;
import com.allgenda.framework.model.StatusParticipacao;

// Política usada nos testes unitários do núcleo, sem depender de nenhuma aplicação: DONO pode tudo;
// MEMBRO vê o conteúdo, cria encontros, eventos e anotações e usa o assistente (a mesma matriz do AllGenda).
public final class PoliticaDeTeste {

    public static final Papel DONO = new Papel("DONO");
    public static final Papel MEMBRO = new Papel("MEMBRO");

    private PoliticaDeTeste() {
    }

    public static PoliticaDeAcesso politica() {
        return PoliticaDeAcessoDeclarativa.construtor()
                .papel(DONO, Acao.values())
                .papel(MEMBRO, Acao.VER_CONTEUDO, Acao.CRIAR_ENCONTRO, Acao.CRIAR_EVENTO, Acao.CRIAR_ANOTACAO,
                        Acao.USAR_ASSISTENTE)
                .papelDoCriador(DONO)
                .papelDoConvidado(MEMBRO)
                .statusAoIngressar(StatusParticipacao.PENDENTE)
                .construir();
    }

    public static Participacao participacao(Papel papel, StatusParticipacao status) {
        Participacao participacao = new Participacao();
        participacao.setPapel(papel);
        participacao.setStatus(status);
        return participacao;
    }

    // o que o repositório devolve para um membro com o convite aceito
    public static Optional<Participacao> membroAceito() {
        return Optional.of(participacao(MEMBRO, StatusParticipacao.ACEITO));
    }
}
