package com.allgenda.framework.acesso;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.allgenda.framework.exception.AcessoNegadoException;
import com.allgenda.framework.model.StatusParticipacao;
import com.allgenda.framework.repository.ParticipacaoRepository;
import com.allgenda.framework.service.UsuarioAutenticado;

// Os mesmos cenários do antigo AcessoDisciplina (participante aceito; só o dono), agora decididos pela política.
class AcessoAoGrupoTest {

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID grupoId = UUID.randomUUID();
    private ParticipacaoRepository participacoes;
    private AcessoAoGrupo acesso;

    @BeforeEach
    void setUp() {
        participacoes = mock(ParticipacaoRepository.class);
        UsuarioAutenticado autenticado = mock(UsuarioAutenticado.class);
        when(autenticado.id()).thenReturn(usuarioId);
        acesso = new AcessoAoGrupo(participacoes, autenticado, PoliticaDeTeste.politica());
    }

    private void participa(Papel papel, StatusParticipacao status) {
        when(participacoes.findByUsuarioIdAndGrupoId(usuarioId, grupoId))
                .thenReturn(Optional.of(PoliticaDeTeste.participacao(papel, status)));
    }

    @Test
    void participanteAceitoPassa() {
        participa(PoliticaDeTeste.MEMBRO, StatusParticipacao.ACEITO);
        assertThatCode(() -> acesso.exigirParticipanteAceito(grupoId)).doesNotThrowAnyException();
    }

    @Test
    void quemNaoParticipaRecebeAcessoNegadoComAMensagemPedida() {
        when(participacoes.findByUsuarioIdAndGrupoId(usuarioId, grupoId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> acesso.exigirParticipanteAceito(grupoId, "sem acesso"))
                .isInstanceOf(AcessoNegadoException.class).hasMessage("sem acesso");
    }

    @Test
    void convitePendenteNaoDaAcesso() {
        participa(PoliticaDeTeste.MEMBRO, StatusParticipacao.PENDENTE);
        assertThatThrownBy(() -> acesso.exigir(Acao.VER_CONTEUDO, grupoId)).isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void donoAceitoPassaNasAcoesDoDono() {
        participa(PoliticaDeTeste.DONO, StatusParticipacao.ACEITO);
        assertThatCode(() -> acesso.exigir(Acao.EDITAR_GRUPO, grupoId)).doesNotThrowAnyException();
    }

    @Test
    void membroPassaNoQueAPoliticaPermite() {
        participa(PoliticaDeTeste.MEMBRO, StatusParticipacao.ACEITO);
        assertThatCode(() -> acesso.exigir(Acao.CRIAR_ANOTACAO, grupoId)).doesNotThrowAnyException();
    }

    @Test
    void membroNaoPassaNasAcoesDoDonoEOuveAMensagemPedida() {
        participa(PoliticaDeTeste.MEMBRO, StatusParticipacao.ACEITO);
        assertThatThrownBy(() -> acesso.exigir(Acao.EDITAR_GRUPO, grupoId, "só o dono"))
                .isInstanceOf(AcessoNegadoException.class).hasMessage("só o dono");
    }

    @Test
    void semParticipacaoNaoPassaNasAcoesDoDono() {
        when(participacoes.findByUsuarioIdAndGrupoId(usuarioId, grupoId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> acesso.exigir(Acao.EDITAR_GRUPO, grupoId)).isInstanceOf(AcessoNegadoException.class);
    }
}
