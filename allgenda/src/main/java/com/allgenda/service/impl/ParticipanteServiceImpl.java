package com.allgenda.service.impl;

import com.allgenda.exception.GrupoNaoEncontradoException;
import com.allgenda.exception.ParticipanteNaoEncontradoException;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.model.PapelParticipacao;
import com.allgenda.model.Participacao;
import com.allgenda.model.StatusParticipacao;
import com.allgenda.model.VisibilidadeAnotacao;
import com.allgenda.repository.AnotacaoRepository;
import com.allgenda.repository.GrupoRepository;
import com.allgenda.repository.ParticipacaoRepository;
import com.allgenda.service.AcessoDisciplina;
import com.allgenda.service.Participante;
import com.allgenda.service.ParticipanteService;
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
    private final AcessoDisciplina acesso;

    public ParticipanteServiceImpl(GrupoRepository grupoRepository,
                                   ParticipacaoRepository participacaoRepository,
                                   AnotacaoRepository anotacaoRepository,
                                   AcessoDisciplina acesso) {
        this.grupoRepository = grupoRepository;
        this.participacaoRepository = participacaoRepository;
        this.anotacaoRepository = anotacaoRepository;
        this.acesso = acesso;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Participante> listar(UUID disciplinaId) {
        exigirDisciplina(disciplinaId);                                                  // 404
        acesso.exigirParticipanteAceito(disciplinaId, "Somente participantes da disciplina veem quem participa."); // 403

        return participacaoRepository.findByGrupoIdAndStatus(disciplinaId, StatusParticipacao.ACEITO).stream()
                .sorted(Comparator
                        .comparing((Participacao participacao) -> participacao.getPapel() != PapelParticipacao.DONO)
                        .thenComparing(participacao -> participacao.getUsuario().getNome(), String.CASE_INSENSITIVE_ORDER))
                .map(participacao -> new Participante(
                        participacao,
                        anotacaoRepository.countByEncontroGrupoIdAndAutorIdAndVisibilidade(
                                disciplinaId, participacao.getUsuario().getId(), VisibilidadeAnotacao.GRUPO)))
                .toList();
    }

    @Override
    @Transactional
    public void remover(UUID disciplinaId, UUID usuarioId) {
        exigirDisciplina(disciplinaId);                                                   // 404
        acesso.exigirDono(disciplinaId, "Somente o dono pode remover participantes da disciplina."); // 403

        Participacao participacao = participacaoRepository.findByUsuarioIdAndGrupoId(usuarioId, disciplinaId)
                .filter(encontrada -> encontrada.getStatus() == StatusParticipacao.ACEITO)
                .orElseThrow(() -> new ParticipanteNaoEncontradoException(usuarioId));    // 404
        if (participacao.getPapel() == PapelParticipacao.DONO) {
            throw new RequisicaoInvalidaException("O dono não pode ser removido da disciplina."); // 400
        }
        participacaoRepository.delete(participacao);
    }

    private void exigirDisciplina(UUID disciplinaId) {
        if (!grupoRepository.existsById(disciplinaId)) {
            throw new GrupoNaoEncontradoException(disciplinaId);
        }
    }
}
