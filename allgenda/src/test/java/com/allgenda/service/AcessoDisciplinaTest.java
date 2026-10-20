package com.allgenda.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.allgenda.exception.AcessoNegadoException;
import com.allgenda.model.PapelParticipacao;
import com.allgenda.model.Participacao;
import com.allgenda.model.StatusParticipacao;
import com.allgenda.repository.ParticipacaoRepository;

class AcessoDisciplinaTest {

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID disciplinaId = UUID.randomUUID();
    private ParticipacaoRepository participacoes;
    private AcessoDisciplina acesso;

    @BeforeEach
    void setUp() {
        participacoes = mock(ParticipacaoRepository.class);
        UsuarioAutenticado autenticado = mock(UsuarioAutenticado.class);
        when(autenticado.id()).thenReturn(usuarioId);
        acesso = new AcessoDisciplina(participacoes, autenticado);
    }

    @Test
    void participanteAceitoPassa() {
        when(participacoes.existsByUsuarioIdAndDisciplinaIdAndStatus(usuarioId, disciplinaId, StatusParticipacao.ACEITO))
                .thenReturn(true);
        assertThatCode(() -> acesso.exigirParticipanteAceito(disciplinaId)).doesNotThrowAnyException();
    }

    @Test
    void quemNaoParticipaRecebeAcessoNegadoComAMensagemPedida() {
        when(participacoes.existsByUsuarioIdAndDisciplinaIdAndStatus(usuarioId, disciplinaId, StatusParticipacao.ACEITO))
                .thenReturn(false);
        assertThatThrownBy(() -> acesso.exigirParticipanteAceito(disciplinaId, "sem acesso"))
                .isInstanceOf(AcessoNegadoException.class).hasMessage("sem acesso");
    }

    @Test
    void donoAceitoPassa() {
        when(participacoes.findByUsuarioIdAndDisciplinaId(usuarioId, disciplinaId))
                .thenReturn(Optional.of(participacao(PapelParticipacao.DONO, StatusParticipacao.ACEITO)));
        assertThatCode(() -> acesso.exigirDono(disciplinaId)).doesNotThrowAnyException();
    }

    @Test
    void membroNaoPassaComoDono() {
        when(participacoes.findByUsuarioIdAndDisciplinaId(usuarioId, disciplinaId))
                .thenReturn(Optional.of(participacao(PapelParticipacao.MEMBRO, StatusParticipacao.ACEITO)));
        assertThatThrownBy(() -> acesso.exigirDono(disciplinaId)).isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void semParticipacaoNaoPassaComoDono() {
        when(participacoes.findByUsuarioIdAndDisciplinaId(usuarioId, disciplinaId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> acesso.exigirDono(disciplinaId)).isInstanceOf(AcessoNegadoException.class);
    }

    private Participacao participacao(PapelParticipacao papel, StatusParticipacao status) {
        Participacao p = new Participacao();
        p.setPapel(papel);
        p.setStatus(status);
        return p;
    }
}
