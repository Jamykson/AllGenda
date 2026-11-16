package com.allgenda.framework.acesso;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.allgenda.exception.AcessoNegadoException;
import com.allgenda.model.Participacao;
import com.allgenda.model.StatusParticipacao;
import com.allgenda.repository.ParticipacaoRepository;
import com.allgenda.service.UsuarioAutenticado;

// Responde "a pessoa logada pode fazer isto neste grupo?". A parte fixa (precisa ter participação ACEITO)
// fica aqui; a parte variável (o papel dela permite a ação?) é perguntada à PoliticaDeAcesso da aplicação.
// Quem chama pode passar a própria mensagem, para o 403 continuar dizendo o que a pessoa tentou fazer.
@Component
public class AcessoAoGrupo {

    private static final String SEM_ACESSO = "Somente participantes do grupo têm acesso a ele.";
    private static final String SEM_PERMISSAO = "Você não tem permissão para fazer isso neste grupo.";

    private final ParticipacaoRepository participacaoRepository;
    private final UsuarioAutenticado usuarioAutenticado;
    private final PoliticaDeAcesso politica;

    public AcessoAoGrupo(ParticipacaoRepository participacaoRepository, UsuarioAutenticado usuarioAutenticado,
                         PoliticaDeAcesso politica) {
        this.participacaoRepository = participacaoRepository;
        this.usuarioAutenticado = usuarioAutenticado;
        this.politica = politica;
    }

    public void exigirParticipanteAceito(UUID grupoId) {
        exigirParticipanteAceito(grupoId, SEM_ACESSO);
    }

    // sem participação, pendente ou recusada: sem acesso (403)
    public void exigirParticipanteAceito(UUID grupoId, String mensagem) {
        participacaoAceita(grupoId, mensagem);
    }

    public void exigir(Acao acao, UUID grupoId) {
        exigir(acao, grupoId, SEM_PERMISSAO);
    }

    // precisa ser participante aceito e ter um papel que a política deixa fazer a ação (403)
    public void exigir(Acao acao, UUID grupoId, String mensagem) {
        Participacao participacao = participacaoAceita(grupoId, mensagem);
        if (!politica.permite(participacao.getPapel(), acao)) {
            throw new AcessoNegadoException(mensagem);
        }
    }

    private Participacao participacaoAceita(UUID grupoId, String mensagem) {
        return participacaoRepository.findByUsuarioIdAndGrupoId(usuarioAutenticado.id(), grupoId)
                .filter(participacao -> participacao.getStatus() == StatusParticipacao.ACEITO)
                .orElseThrow(() -> new AcessoNegadoException(mensagem));
    }
}
