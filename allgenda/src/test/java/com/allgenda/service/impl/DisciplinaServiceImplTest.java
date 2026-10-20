package com.allgenda.service.impl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InOrder;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.allgenda.dto.request.DisciplinaRequestDTO;
import com.allgenda.dto.response.DisciplinaResponseDTO;
import com.allgenda.exception.AcessoNegadoException;
import com.allgenda.exception.DisciplinaNaoEncontradaException;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.mapper.DisciplinaMapper;
import com.allgenda.model.Disciplina;
import com.allgenda.model.PapelParticipacao;
import com.allgenda.model.Participacao;
import com.allgenda.model.StatusParticipacao;
import com.allgenda.model.Usuario;
import com.allgenda.repository.DisciplinaRepository;
import com.allgenda.repository.ParticipacaoRepository;
import com.allgenda.repository.ImagemAnotacaoRepository;
import com.allgenda.repository.ImagemAulaRepository;
import com.allgenda.service.AcessoDisciplina;
import com.allgenda.service.ArmazenamentoService;
import com.allgenda.service.RemocaoArquivos;
import com.allgenda.service.UsuarioAutenticado;

@ExtendWith(MockitoExtension.class)
class DisciplinaServiceImplTest {

    @Mock
    private DisciplinaRepository repository;
    @Mock
    private ParticipacaoRepository participacaoRepository;
    @Mock
    private ImagemAnotacaoRepository imagemRepository;
    @Mock
    private ImagemAulaRepository imagemAulaRepository;
    @Mock
    private ArmazenamentoService armazenamento;
    @Mock
    private UsuarioAutenticado usuarioAutenticado;
    @Mock
    private AcessoDisciplina acessoDisciplina;

    private DisciplinaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DisciplinaServiceImpl(repository, participacaoRepository, imagemRepository, imagemAulaRepository, new RemocaoArquivos(armazenamento), new DisciplinaMapper(), usuarioAutenticado, acessoDisciplina);
    }

    @Test
    void cadastraDisciplinaComNomeEDescricaoNormalizados() {
        when(usuarioAutenticado.obter()).thenReturn(new Usuario());
        when(repository.save(any(Disciplina.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var criada = service.cadastrar(
            new DisciplinaRequestDTO("  Física  ", "  Mecânica clássica  ")
        );

        assertThat(criada.nome()).isEqualTo("Física");
        assertThat(criada.descricao()).isEqualTo("Mecânica clássica");
        verify(repository).save(any(Disciplina.class));
    }

    @Test
    void rejeitaNomeEmBranco() {
        assertThatThrownBy(() -> service.cadastrar(new DisciplinaRequestDTO("  ", null)))
            .isInstanceOf(RequisicaoInvalidaException.class);

        verify(repository, never()).save(any(Disciplina.class));
    }

    @Test
    void rejeitaDescricaoMaiorQueQuinhentosCaracteres() {
        String descricao = "a".repeat(501);

        assertThatThrownBy(() -> service.cadastrar(new DisciplinaRequestDTO("Física", descricao)))
            .isInstanceOf(RequisicaoInvalidaException.class);

        verify(repository, never()).save(any(Disciplina.class));
    }

    @Test
    void quemCriaADisciplinaViraDonoComParticipacaoAceita() {
        var usuario = new Usuario();
        when(usuarioAutenticado.obter()).thenReturn(usuario);
        when(repository.save(any(Disciplina.class))).thenAnswer(i -> i.getArgument(0));

        service.cadastrar(new DisciplinaRequestDTO("Física", null));

        var captor = org.mockito.ArgumentCaptor.forClass(Disciplina.class);
        verify(repository).save(captor.capture());
        verify(participacaoRepository).save(any(com.allgenda.model.Participacao.class));
        assertThat(captor.getValue().getParticipacoes()).singleElement().satisfies(p -> {
            assertThat(p.getUsuario()).isSameAs(usuario);
            assertThat(p.getPapel()).isEqualTo(PapelParticipacao.DONO);
            assertThat(p.getStatus()).isEqualTo(StatusParticipacao.ACEITO);
        });
    }

    @Test
    void listaSomenteAsDisciplinasDeParticipacaoAceita() {
        var usuarioId = java.util.UUID.randomUUID();
        var disciplina = new Disciplina();
        disciplina.setNome("PDS");
        var participacao = new Participacao();
        participacao.setDisciplina(disciplina);
        when(usuarioAutenticado.id()).thenReturn(usuarioId);
        when(participacaoRepository.findByUsuarioIdAndStatus(usuarioId, StatusParticipacao.ACEITO))
            .thenReturn(List.of(participacao));

        assertThat(service.listarDoUsuario()).extracting(DisciplinaResponseDTO::nome).containsExactly("PDS");
        verify(repository, never()).findAll();
    }

    @Test
    void quemNaoEhDonoNaoExcluiADisciplina() {
        var id = java.util.UUID.randomUUID();
        when(repository.findById(id)).thenReturn(java.util.Optional.of(new Disciplina()));
        doThrow(new AcessoNegadoException("Somente o dono pode excluir a disciplina."))
            .when(acessoDisciplina).exigirDono(id, "Somente o dono pode excluir a disciplina.");

        assertThatThrownBy(() -> service.excluir(id)).isInstanceOf(AcessoNegadoException.class);

        verify(participacaoRepository, never()).deleteByDisciplinaId(any());
        verify(repository, never()).delete(any(Disciplina.class));
    }

    @Test
    void quemNaoParticipaNaoBuscaADisciplinaPeloId() {
        var id = java.util.UUID.randomUUID();
        when(repository.findById(id)).thenReturn(java.util.Optional.of(new Disciplina()));
        doThrow(new AcessoNegadoException("Somente participantes da disciplina têm acesso a ela."))
            .when(acessoDisciplina).exigirParticipanteAceito(id);

        assertThatThrownBy(() -> service.buscarPorId(id)).isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void excluiDisciplinaExistente() {
        var id = java.util.UUID.randomUUID();
        var disciplina = new Disciplina();
        when(repository.findById(id)).thenReturn(java.util.Optional.of(disciplina));

        service.excluir(id);

        InOrder ordem = inOrder(participacaoRepository, repository);
        ordem.verify(participacaoRepository).deleteByDisciplinaId(id);
        ordem.verify(repository).delete(disciplina);
    }

    @Test
    void rejeitaExclusaoDeDisciplinaInexistente() {
        var id = java.util.UUID.randomUUID();
        when(repository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.excluir(id))
            .isInstanceOf(DisciplinaNaoEncontradaException.class);

        verify(repository, never()).delete(any(Disciplina.class));
    }

    @Test
    void excluirDisciplinaApagaOsArquivosDasImagensDepoisDoRegistro() {
        var id = java.util.UUID.randomUUID();
        var disciplina = new Disciplina();
        when(repository.findById(id)).thenReturn(java.util.Optional.of(disciplina));
        when(imagemRepository.findChavesArmazenamentoByDisciplinaId(id)).thenReturn(List.of("a.png", "b.jpg"));
        when(imagemAulaRepository.findChavesArmazenamentoByDisciplinaId(id)).thenReturn(List.of("avulsa.webp"));

        service.excluir(id);

        InOrder ordem = inOrder(repository, armazenamento);
        ordem.verify(repository).delete(disciplina);
        ordem.verify(armazenamento).remover("a.png");
        ordem.verify(armazenamento).remover("b.jpg");
        ordem.verify(armazenamento).remover("avulsa.webp");
    }

    @Test
    void seAExclusaoFalharOsArquivosFicam() {
        var id = java.util.UUID.randomUUID();
        var disciplina = new Disciplina();
        when(repository.findById(id)).thenReturn(java.util.Optional.of(disciplina));
        when(imagemRepository.findChavesArmazenamentoByDisciplinaId(id)).thenReturn(List.of("a.png"));
        doThrow(new DataIntegrityViolationException("FK")).when(repository).delete(disciplina);

        assertThatThrownBy(() -> service.excluir(id))
            .isInstanceOf(DataIntegrityViolationException.class);

        verify(armazenamento, never()).remover(anyString());
    }
}
