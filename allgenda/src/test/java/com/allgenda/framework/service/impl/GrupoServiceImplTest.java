package com.allgenda.framework.service.impl;

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

import com.allgenda.framework.exception.AcessoNegadoException;
import com.allgenda.framework.exception.GrupoNaoEncontradoException;
import com.allgenda.framework.exception.RequisicaoInvalidaException;
import com.allgenda.aplicacao.allgenda.model.Disciplina;
import com.allgenda.framework.model.Grupo;
import com.allgenda.framework.model.Participacao;
import com.allgenda.framework.model.StatusParticipacao;
import com.allgenda.framework.model.Usuario;
import com.allgenda.framework.repository.GrupoRepository;
import com.allgenda.framework.repository.ParticipacaoRepository;
import com.allgenda.framework.repository.RecorrenciaRepository;
import com.allgenda.framework.repository.ImagemAnotacaoRepository;
import com.allgenda.framework.repository.ImagemEncontroRepository;
import com.allgenda.framework.acesso.AcessoAoGrupo;
import com.allgenda.framework.acesso.Acao;
import com.allgenda.framework.acesso.PoliticaDeTeste;
import com.allgenda.framework.service.ArmazenamentoService;
import com.allgenda.aplicacao.allgenda.FabricaDeDisciplina;
import com.allgenda.framework.extensao.DadosDoGrupo;
import com.allgenda.framework.extensao.RegistroDeTipos;
import com.allgenda.framework.service.RemocaoArquivos;
import com.allgenda.framework.service.UsuarioAutenticado;

@ExtendWith(MockitoExtension.class)
class GrupoServiceImplTest {

    @Mock
    private GrupoRepository repository;
    @Mock
    private ParticipacaoRepository participacaoRepository;
    @Mock
    private RecorrenciaRepository recorrenciaRepository;
    @Mock
    private ImagemAnotacaoRepository imagemRepository;
    @Mock
    private ImagemEncontroRepository imagemEncontroRepository;
    @Mock
    private ArmazenamentoService armazenamento;
    @Mock
    private UsuarioAutenticado usuarioAutenticado;
    @Mock
    private AcessoAoGrupo acessoAoGrupo;

    private GrupoServiceImpl service;

    @BeforeEach
    void setUp() {
        // o tipo vem da aplicação; aqui basta a fábrica de disciplina do AllGenda
        RegistroDeTipos registro = new RegistroDeTipos();
        registro.registrar(new FabricaDeDisciplina());
        service = new GrupoServiceImpl(repository, participacaoRepository, recorrenciaRepository, imagemRepository, imagemEncontroRepository, new RemocaoArquivos(armazenamento), usuarioAutenticado, acessoAoGrupo, PoliticaDeTeste.politica(), registro);
    }

    @Test
    void cadastraDisciplinaComNomeEDescricaoNormalizados() {
        when(usuarioAutenticado.obter()).thenReturn(new Usuario());
        when(repository.save(any(Disciplina.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var criada = service.criar("DISCIPLINA", 
            new DadosDoGrupo("  Física  ", "  Mecânica clássica  ")
        );

        assertThat(criada.getNome()).isEqualTo("Física");
        assertThat(criada.getDescricao()).isEqualTo("Mecânica clássica");
        verify(repository).save(any(Disciplina.class));
    }

    @Test
    void rejeitaNomeEmBranco() {
        assertThatThrownBy(() -> service.criar("DISCIPLINA", new DadosDoGrupo("  ", null)))
            .isInstanceOf(RequisicaoInvalidaException.class);

        verify(repository, never()).save(any(Disciplina.class));
    }

    @Test
    void rejeitaDescricaoMaiorQueQuinhentosCaracteres() {
        String descricao = "a".repeat(501);

        assertThatThrownBy(() -> service.criar("DISCIPLINA", new DadosDoGrupo("Física", descricao)))
            .isInstanceOf(RequisicaoInvalidaException.class);

        verify(repository, never()).save(any(Disciplina.class));
    }

    @Test
    void quemCriaADisciplinaViraDonoComParticipacaoAceita() {
        var usuario = new Usuario();
        when(usuarioAutenticado.obter()).thenReturn(usuario);
        when(repository.save(any(Disciplina.class))).thenAnswer(i -> i.getArgument(0));

        service.criar("DISCIPLINA", new DadosDoGrupo("Física", null));

        var captor = org.mockito.ArgumentCaptor.forClass(Disciplina.class);
        verify(repository).save(captor.capture());
        verify(participacaoRepository).save(any(com.allgenda.framework.model.Participacao.class));
        assertThat(captor.getValue().getParticipacoes()).singleElement().satisfies(p -> {
            assertThat(p.getUsuario()).isSameAs(usuario);
            assertThat(p.getPapel()).isEqualTo(PoliticaDeTeste.DONO);
            assertThat(p.getStatus()).isEqualTo(StatusParticipacao.ACEITO);
        });
    }

    @Test
    void listaSomenteAsDisciplinasDeParticipacaoAceita() {
        var usuarioId = java.util.UUID.randomUUID();
        var disciplina = new Disciplina();
        disciplina.setNome("PDS");
        var participacao = new Participacao();
        participacao.setGrupo(disciplina);
        when(usuarioAutenticado.id()).thenReturn(usuarioId);
        when(participacaoRepository.findByUsuarioIdAndStatus(usuarioId, StatusParticipacao.ACEITO))
            .thenReturn(List.of(participacao));

        assertThat(service.listarDoUsuario()).extracting(Grupo::getNome).containsExactly("PDS");
        verify(repository, never()).findAll();
    }

    @Test
    void quemNaoEhDonoNaoExcluiADisciplina() {
        var id = java.util.UUID.randomUUID();
        when(repository.findById(id)).thenReturn(java.util.Optional.of(new Disciplina()));
        doThrow(new AcessoNegadoException("Você não tem permissão para excluir o grupo."))
            .when(acessoAoGrupo).exigir(Acao.EXCLUIR_GRUPO, id, "Você não tem permissão para excluir o grupo.");

        assertThatThrownBy(() -> service.excluir(id)).isInstanceOf(AcessoNegadoException.class);

        verify(participacaoRepository, never()).deleteByGrupoId(any());
        verify(repository, never()).delete(any(Disciplina.class));
    }

    @Test
    void quemNaoParticipaNaoBuscaADisciplinaPeloId() {
        var id = java.util.UUID.randomUUID();
        when(repository.findById(id)).thenReturn(java.util.Optional.of(new Disciplina()));
        doThrow(new AcessoNegadoException("Somente participantes do grupo têm acesso a ele."))
            .when(acessoAoGrupo).exigir(Acao.VER_CONTEUDO, id, "Somente participantes do grupo têm acesso a ele.");

        assertThatThrownBy(() -> service.buscarPorId(id)).isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void excluiDisciplinaExistente() {
        var id = java.util.UUID.randomUUID();
        var disciplina = new Disciplina();
        when(repository.findById(id)).thenReturn(java.util.Optional.of(disciplina));

        service.excluir(id);

        InOrder ordem = inOrder(participacaoRepository, repository);
        ordem.verify(participacaoRepository).deleteByGrupoId(id);
        ordem.verify(repository).delete(disciplina);
    }

    @Test
    void rejeitaExclusaoDeDisciplinaInexistente() {
        var id = java.util.UUID.randomUUID();
        when(repository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.excluir(id))
            .isInstanceOf(GrupoNaoEncontradoException.class);

        verify(repository, never()).delete(any(Disciplina.class));
    }

    @Test
    void excluirDisciplinaApagaOsArquivosDasImagensDepoisDoRegistro() {
        var id = java.util.UUID.randomUUID();
        var disciplina = new Disciplina();
        when(repository.findById(id)).thenReturn(java.util.Optional.of(disciplina));
        when(imagemRepository.findChavesArmazenamentoByGrupoId(id)).thenReturn(List.of("a.png", "b.jpg"));
        when(imagemEncontroRepository.findChavesArmazenamentoByGrupoId(id)).thenReturn(List.of("avulsa.webp"));

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
        when(imagemRepository.findChavesArmazenamentoByGrupoId(id)).thenReturn(List.of("a.png"));
        doThrow(new DataIntegrityViolationException("FK")).when(repository).delete(disciplina);

        assertThatThrownBy(() -> service.excluir(id))
            .isInstanceOf(DataIntegrityViolationException.class);

        verify(armazenamento, never()).remover(anyString());
    }
}
