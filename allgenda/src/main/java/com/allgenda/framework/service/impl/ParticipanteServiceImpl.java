package com.allgenda.framework.service.impl;

import com.allgenda.framework.exception.GrupoNaoEncontradoException;
import com.allgenda.framework.exception.ParticipanteNaoEncontradoException;
import com.allgenda.framework.exception.RequisicaoInvalidaException;
import com.allgenda.framework.model.Participacao;
import com.allgenda.framework.model.StatusParticipacao;
import com.allgenda.framework.model.VisibilidadeAnotacao;
import com.allgenda.framework.repository.AnotacaoRepository;
import com.allgenda.framework.repository.GrupoRepository;
import com.allgenda.framework.repository.ParticipacaoRepository;
import com.allgenda.framework.acesso.AcessoAoGrupo;
import com.allgenda.framework.acesso.Acao;
import com.allgenda.framework.acesso.PoliticaDeAcesso;
import com.allgenda.framework.service.Participante;
import com.allgenda.framework.service.ParticipanteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ParticipanteServiceImpl implements ParticipanteService {

    private final GrupoRepository grupoRepository;
    private final ParticipacaoRepository participacaoRepository;
    private final AnotacaoRepository anotacaoRepository;
    private final AcessoAoGrupo acesso;
    private final PoliticaDeAcesso politica;

    public ParticipanteServiceImpl(GrupoRepository grupoRepository,
                                   ParticipacaoRepository participacaoRepository,
                                   AnotacaoRepository anotacaoRepository,
                                   AcessoAoGrupo acesso,
                                   PoliticaDeAcesso politica) {
        this.grupoRepository = grupoRepository;
        this.participacaoRepository = participacaoRepository;
        this.anotacaoRepository = anotacaoRepository;
        this.acesso = acesso;
        this.politica = politica;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Participante> listar(UUID grupoId) {
        exigirGrupo(grupoId);                                                  // 404
        acesso.exigir(Acao.VER_CONTEUDO, grupoId, "Somente participantes do grupo veem quem participa."); // 403

        return participacaoRepository.findByGrupoIdAndStatus(grupoId, StatusParticipacao.ACEITO).stream()
                .sorted(Comparator
                        .comparing((Participacao participacao) -> !ehCriador(participacao))
                        .thenComparing(participacao -> participacao.getUsuario().getNome(), String.CASE_INSENSITIVE_ORDER))
                .map(participacao -> new Participante(
                        participacao,
                        anotacaoRepository.countByEncontroGrupoIdAndAutorIdAndVisibilidade(
                                grupoId, participacao.getUsuario().getId(), VisibilidadeAnotacao.GRUPO)))
                .toList();
    }

    @Override
    @Transactional
    public void remover(UUID grupoId, UUID usuarioId) {
        exigirGrupo(grupoId);                                                   // 404
        acesso.exigir(Acao.REMOVER_MEMBRO, grupoId, "Você não tem permissão para remover participantes deste grupo."); // 403

        Participacao participacao = participacaoRepository.findByUsuarioIdAndGrupoId(usuarioId, grupoId)
                .filter(encontrada -> encontrada.getStatus() == StatusParticipacao.ACEITO)
                .orElseThrow(() -> new ParticipanteNaoEncontradoException(usuarioId));    // 404
        // regra fixa do núcleo, não da política: quem criou o grupo não sai dele
        if (ehCriador(participacao)) {
            throw new RequisicaoInvalidaException("Quem criou o grupo não pode ser removido dele."); // 400
        }
        participacaoRepository.delete(participacao);
    }

    private boolean ehCriador(Participacao participacao) {
        return politica.papelDoCriador().equals(participacao.getPapel());
    }

    private void exigirGrupo(UUID grupoId) {
        if (!grupoRepository.existsById(grupoId)) {
            throw new GrupoNaoEncontradoException(grupoId);
        }
    }
}
