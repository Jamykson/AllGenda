package com.allgenda.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.allgenda.exception.AcessoNegadoException;
import com.allgenda.model.PapelParticipacao;
import com.allgenda.model.StatusParticipacao;
import com.allgenda.repository.ParticipacaoRepository;

// Regras de "quem pode mexer nesta disciplina", baseadas na participação da pessoa logada.
// Concentra a checagem que antes se repetia em vários services; quem chama pode passar a própria
// mensagem de erro, para o 403 continuar dizendo o que a pessoa tentou fazer.
@Component
public class AcessoDisciplina {

    private final ParticipacaoRepository participacaoRepository;
    private final UsuarioAutenticado usuarioAutenticado;

    public AcessoDisciplina(ParticipacaoRepository participacaoRepository, UsuarioAutenticado usuarioAutenticado) {
        this.participacaoRepository = participacaoRepository;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    public void exigirParticipanteAceito(UUID disciplinaId) {
        exigirParticipanteAceito(disciplinaId, "Somente participantes da disciplina têm acesso a ela.");
    }

    // dono e membro têm participação ACEITO; sem participação, pendente ou recusada: sem acesso (403)
    public void exigirParticipanteAceito(UUID disciplinaId, String mensagem) {
        boolean participa = participacaoRepository.existsByUsuarioIdAndDisciplinaIdAndStatus(
                usuarioAutenticado.id(), disciplinaId, StatusParticipacao.ACEITO);
        if (!participa) {
            throw new AcessoNegadoException(mensagem);
        }
    }

    public void exigirDono(UUID disciplinaId) {
        exigirDono(disciplinaId, "Somente o dono da disciplina pode fazer isso.");
    }

    // só o dono (com participação aceita) passa (403)
    public void exigirDono(UUID disciplinaId, String mensagem) {
        boolean dono = participacaoRepository.findByUsuarioIdAndDisciplinaId(usuarioAutenticado.id(), disciplinaId)
                .filter(p -> p.getPapel() == PapelParticipacao.DONO && p.getStatus() == StatusParticipacao.ACEITO)
                .isPresent();
        if (!dono) {
            throw new AcessoNegadoException(mensagem);
        }
    }
}
