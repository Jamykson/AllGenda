package com.allgenda.aplicacao.allgenda;

import static com.allgenda.aplicacao.allgenda.model.PapelAllGenda.DONO;
import static com.allgenda.aplicacao.allgenda.model.PapelAllGenda.MEMBRO;

import com.allgenda.framework.acesso.Acao;
import com.allgenda.framework.acesso.PoliticaDeAcesso;
import com.allgenda.framework.acesso.PoliticaDeAcessoDeclarativa;
import com.allgenda.model.StatusParticipacao;

// Política de acesso do AllGenda (ponto variável 2), declarada como matriz papel -> ações:
// o DONO pode tudo; o MEMBRO vê o conteúdo, cria aulas, avaliações e anotações (com tags e imagens) e usa
// o assistente. Editar, cancelar e excluir aulas, convidar, remover membros, editar e excluir a
// disciplina e definir a recorrência ficam só com o dono. O ingresso é por convite, que começa PENDENTE.
public final class PoliticaAllGenda {

    private PoliticaAllGenda() {
    }

    public static PoliticaDeAcesso criar() {
        return PoliticaDeAcessoDeclarativa.construtor()
                .papel(DONO, Acao.values())
                .papel(MEMBRO,
                        Acao.VER_CONTEUDO,
                        Acao.CRIAR_ENCONTRO,
                        Acao.CRIAR_EVENTO,
                        Acao.CRIAR_ANOTACAO,
                        Acao.USAR_ASSISTENTE)
                .papelDoCriador(DONO)
                .papelDoConvidado(MEMBRO)
                .statusAoIngressar(StatusParticipacao.PENDENTE)
                .construir();
    }
}
