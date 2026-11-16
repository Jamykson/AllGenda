package com.allgenda.framework.service.impl;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.junit.jupiter.api.AfterEach;
import com.allgenda.framework.exception.AcessoNegadoException;
import com.allgenda.framework.exception.AnotacaoNaoEncontradaException;
import com.allgenda.framework.exception.EncontroNaoEncontradoException;
import com.allgenda.framework.exception.GrupoNaoEncontradoException;
import com.allgenda.framework.exception.RequisicaoInvalidaException;
import com.allgenda.framework.exception.UsuarioNaoEncontradoException;
import com.allgenda.framework.model.Anotacao;
import com.allgenda.aplicacao.allgenda.model.Aula;
import com.allgenda.aplicacao.allgenda.model.Disciplina;
import com.allgenda.framework.model.ImagemAnotacao;
import com.allgenda.framework.model.Tag;
import com.allgenda.framework.model.Usuario;
import com.allgenda.framework.model.Participacao;
import com.allgenda.framework.model.StatusParticipacao;
import com.allgenda.framework.repository.AnotacaoRepository;
import com.allgenda.framework.repository.EncontroRepository;
import com.allgenda.framework.repository.GrupoRepository;
import com.allgenda.framework.repository.ParticipacaoRepository;
import com.allgenda.framework.repository.UsuarioRepository;
import com.allgenda.framework.acesso.AcessoAoGrupo;
import com.allgenda.framework.acesso.PoliticaDeTeste;
import com.allgenda.framework.service.ArmazenamentoService;
import com.allgenda.framework.service.NovaAnotacao;
import com.allgenda.framework.service.RemocaoArquivos;
import com.allgenda.framework.service.TagService;
import com.allgenda.framework.service.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnotacaoServiceImplTest {

    @Mock
    private AnotacaoRepository anotacaoRepository;
    @Mock
    private EncontroRepository encontroRepository;
    @Mock
    private GrupoRepository grupoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock 
    private ParticipacaoRepository participacaoRepository;
    @Mock
    private TagService tagService;
    @Mock
    private ArmazenamentoService armazenamento;


    private AnotacaoServiceImpl service;

    private final UUID aulaId = UUID.randomUUID();
    private final UUID disciplinaDaAulaId = UUID.randomUUID();
    private Aula aula;
    private final UUID usuarioId = UUID.randomUUID();
    private Usuario usuarioPadrao;

    @BeforeEach
    void setUp() {
        // UsuarioAutenticado real: lê o usuário do SecurityContext preparado abaixo e o busca no repositório simulado
        UsuarioAutenticado autenticado = new UsuarioAutenticado(usuarioRepository);
        service = new AnotacaoServiceImpl(anotacaoRepository, encontroRepository, grupoRepository, participacaoRepository,
                autenticado, new AcessoAoGrupo(participacaoRepository, autenticado, PoliticaDeTeste.politica()), tagService,
                new RemocaoArquivos(armazenamento));

        Disciplina disciplina = new Disciplina();
        disciplina.setNome("PDS");
        ReflectionTestUtils.setField(disciplina, "id", disciplinaDaAulaId);
        aula = new Aula();
        aula.setTopico("Padrões de projeto");
        aula.setGrupo(disciplina);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuarioId.toString(), null, List.of()));
        usuarioPadrao = new Usuario();
        usuarioPadrao.setNome("Maria");
        ReflectionTestUtils.setField(usuarioPadrao, "id", usuarioId);
    }

    @AfterEach
    void limparContexto() { SecurityContextHolder.clearContext(); }

    private static Tag tag(String nome) {
        Tag tag = new Tag();
        tag.setNome(nome);
        return tag;
    }

    // o service devolve a entidade; as tags ficam num Set, então a comparação usa só os nomes
    private static List<String> nomesDasTags(Anotacao anotacao) {
        return anotacao.getTags().stream().map(Tag::getNome).toList();
    }

    private Anotacao anotacao(String conteudo, Tag... tags) {
        Anotacao anotacao = new Anotacao();
        anotacao.setConteudo(conteudo);
        anotacao.setEncontro(aula);
        anotacao.setAutor(usuarioPadrao);
        for (Tag tag : tags) {
            anotacao.addTag(tag);
        }
        return anotacao;
    }

    @Test
    void cadastraAnotacaoComUsuarioAutenticadoETags() {
        participantesDaDisciplinaDaAula();
        when(encontroRepository.findById(aulaId)).thenReturn(Optional.of(aula));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioPadrao));
        when(tagService.buscarOuCriar(aula.getGrupo(), List.of("java", "poo"))).thenReturn(new LinkedHashSet<>(List.of(tag("java"), tag("poo"))));
        when(anotacaoRepository.save(any(Anotacao.class))).thenAnswer(inv -> inv.getArgument(0));

        Anotacao resposta = service.cadastrar(new NovaAnotacao(aulaId, null, "Strategy e Observer", List.of("java", "poo"), null));

        assertThat(resposta.getConteudo()).isEqualTo("Strategy e Observer");
        assertThat(resposta.getAutor().getId()).isEqualTo(usuarioId);
        assertThat(resposta.getAutor().getNome()).isEqualTo("Maria");
        assertThat(resposta.getEncontro().getTopico()).isEqualTo("Padrões de projeto");
        assertThat(resposta.getEncontro().getGrupo().getNome()).isEqualTo("PDS");
        assertThat(nomesDasTags(resposta)).containsExactlyInAnyOrder("java", "poo");
    }

    @Test
    void cadastraAnotacaoSemTags() {
        participantesDaDisciplinaDaAula();
        when(encontroRepository.findById(aulaId)).thenReturn(Optional.of(aula));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioPadrao));
        when(tagService.buscarOuCriar(aula.getGrupo(), null)).thenReturn(Set.of());
        when(anotacaoRepository.save(any(Anotacao.class))).thenAnswer(inv -> inv.getArgument(0));

        Anotacao resposta = service.cadastrar(new NovaAnotacao(aulaId, null, "Sem tags", null, null));

        assertThat(nomesDasTags(resposta)).isEmpty();
    }

    @Test
    void usaAutorAutenticadoQuandoExiste() {
        participantesDaDisciplinaDaAula();
        UUID autorId = usuarioId;
        Usuario autor = new Usuario();
        autor.setNome("Maria");
        when(encontroRepository.findById(aulaId)).thenReturn(Optional.of(aula));
        when(usuarioRepository.findById(autorId)).thenReturn(Optional.of(autor));
        when(tagService.buscarOuCriar(aula.getGrupo(), null)).thenReturn(Set.of());
        when(anotacaoRepository.save(any(Anotacao.class))).thenAnswer(inv -> inv.getArgument(0));

        Anotacao resposta = service.cadastrar(new NovaAnotacao(aulaId, null, "Texto", null, null));

        assertThat(resposta.getAutor().getNome()).isEqualTo("Maria");
    }

    @Test
    void autorInexistenteLancaErroTratado() {
        participantesDaDisciplinaDaAula();
        UUID autorId = usuarioId;
        when(encontroRepository.findById(aulaId)).thenReturn(Optional.of(aula));
        when(usuarioRepository.findById(autorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cadastrar(new NovaAnotacao(aulaId, null, "Texto", null, null)))
                .isInstanceOf(UsuarioNaoEncontradoException.class);
        verify(anotacaoRepository, never()).save(any());
    }

    @Test
    void cadastrarSemParticipacaoAceitaLancaAcessoNegado() {
        when(encontroRepository.findById(aulaId)).thenReturn(Optional.of(aula));

        assertThatThrownBy(() -> service.cadastrar(new NovaAnotacao(aulaId, null, "Texto", null, null)))
                .isInstanceOf(AcessoNegadoException.class);
        verify(anotacaoRepository, never()).save(any());
    }

    @Test
    void aulaInexistenteLancaErroTratado() {
        when(encontroRepository.findById(aulaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cadastrar(new NovaAnotacao(aulaId, null, "Texto", null, null)))
                .isInstanceOf(EncontroNaoEncontradoException.class);
        verify(anotacaoRepository, never()).save(any());
    }

    @Test
    void tituloEConteudoVaziosLancaRequisicaoInvalida() {
        assertThatThrownBy(() -> service.cadastrar(new NovaAnotacao(aulaId, "  ", "   ", null, null)))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessage("Escreva um título ou um texto para a anotação.");
        verify(anotacaoRepository, never()).save(any());
    }

    @Test
    void cadastraAnotacaoSoComTitulo() {
        participantesDaDisciplinaDaAula();
        when(encontroRepository.findById(aulaId)).thenReturn(Optional.of(aula));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioPadrao));
        when(tagService.buscarOuCriar(aula.getGrupo(), null)).thenReturn(Set.of());
        when(anotacaoRepository.save(any(Anotacao.class))).thenAnswer(inv -> inv.getArgument(0));

        Anotacao resposta = service.cadastrar(new NovaAnotacao(aulaId, "  Prova 1  ", " ", null, null));

        assertThat(resposta.getTitulo()).isEqualTo("Prova 1");
        assertThat(resposta.getConteudo()).isNull();
    }

    @Test
    void cadastraAnotacaoComTituloEConteudo() {
        participantesDaDisciplinaDaAula();
        when(encontroRepository.findById(aulaId)).thenReturn(Optional.of(aula));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioPadrao));
        when(tagService.buscarOuCriar(aula.getGrupo(), null)).thenReturn(Set.of());
        when(anotacaoRepository.save(any(Anotacao.class))).thenAnswer(inv -> inv.getArgument(0));

        Anotacao resposta = service.cadastrar(new NovaAnotacao(aulaId, "Padrões", "Strategy e Observer", null, null));

        assertThat(resposta.getTitulo()).isEqualTo("Padrões");
        assertThat(resposta.getConteudo()).isEqualTo("Strategy e Observer");
    }

    @Test
    void tituloEmBrancoFicaNulo() {
        participantesDaDisciplinaDaAula();
        when(encontroRepository.findById(aulaId)).thenReturn(Optional.of(aula));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioPadrao));
        when(tagService.buscarOuCriar(aula.getGrupo(), null)).thenReturn(Set.of());
        when(anotacaoRepository.save(any(Anotacao.class))).thenAnswer(inv -> inv.getArgument(0));

        Anotacao resposta = service.cadastrar(new NovaAnotacao(aulaId, "   ", "Texto", null, null));

        assertThat(resposta.getTitulo()).isNull();
        assertThat(resposta.getConteudo()).isEqualTo("Texto");
    }

    @Test
    void tituloLongoDemaisLancaRequisicaoInvalida() {
        assertThatThrownBy(() -> service.cadastrar(new NovaAnotacao(aulaId, "a".repeat(151), "Texto", null, null)))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessage("O título deve ter no máximo 150 caracteres.");
        verify(anotacaoRepository, never()).save(any());
    }

    @Test
    void aulaIdNuloLancaRequisicaoInvalida() {
        assertThatThrownBy(() -> service.cadastrar(new NovaAnotacao(null, null, "Texto", null, null)))
                .isInstanceOf(RequisicaoInvalidaException.class);
    }

    @Test
    void associarTagsAcrescentaAsTagsExistentes() {
        UUID anotacaoId = UUID.randomUUID();
        Anotacao anotacao = new Anotacao();
        anotacao.setConteudo("Texto");
        anotacao.setEncontro(aula);
        anotacao.setAutor(usuarioPadrao);
        anotacao.addTag(tag("java"));
        when(anotacaoRepository.findById(anotacaoId)).thenReturn(Optional.of(anotacao));
        participantesDaDisciplinaDaAula();
        when(tagService.buscarOuCriar(aula.getGrupo(), List.of("poo", "spring"))).thenReturn(new LinkedHashSet<>(List.of(tag("poo"), tag("spring"))));
        when(anotacaoRepository.save(anotacao)).thenReturn(anotacao);

        Anotacao resposta = service.associarTags(anotacaoId, List.of("poo", "spring"));

        assertThat(nomesDasTags(resposta)).containsExactlyInAnyOrder("java", "poo", "spring");
    }

    @Test
    void associarTagsEmAnotacaoInexistenteLancaErroTratado() {
        UUID anotacaoId = UUID.randomUUID();
        when(anotacaoRepository.findById(anotacaoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.associarTags(anotacaoId, List.of("java")))
                .isInstanceOf(AnotacaoNaoEncontradaException.class);
    }

    @Test
    void associarListaVaziaDeTagsLancaRequisicaoInvalida() {
        assertThatThrownBy(() -> service.associarTags(UUID.randomUUID(), List.of()))
                .isInstanceOf(RequisicaoInvalidaException.class);
    }

    @Test
    void quemNaoEhAutorTambemAssociaTags() {
        UUID anotacaoId = UUID.randomUUID();
        Anotacao anotacao = anotacao("Texto", tag("java"));
        when(anotacaoRepository.findById(anotacaoId)).thenReturn(Optional.of(anotacao));
        participantesDaDisciplinaDaAula();
        when(tagService.buscarOuCriar(aula.getGrupo(), List.of("poo"))).thenReturn(new LinkedHashSet<>(List.of(tag("poo"))));
        when(anotacaoRepository.save(anotacao)).thenReturn(anotacao);
        autenticarOutroUsuario();

        Anotacao resposta = service.associarTags(anotacaoId, List.of("poo"));

        assertThat(nomesDasTags(resposta)).containsExactlyInAnyOrder("java", "poo");
    }

    @Test
    void quemNaoParticipaNaoAssociaTags() {
        UUID anotacaoId = UUID.randomUUID();
        Anotacao anotacao = anotacao("Texto", tag("java"));
        when(anotacaoRepository.findById(anotacaoId)).thenReturn(Optional.of(anotacao));

        assertThatThrownBy(() -> service.associarTags(anotacaoId, List.of("poo")))
                .isInstanceOf(AcessoNegadoException.class);
        verify(anotacaoRepository, never()).save(any());
        assertThat(anotacao.getTags()).extracting(Tag::getNome).containsExactly("java");
    }

    @Test
    void quemNaoEhAutorRemoveTagIgnorandoMaiusculas() {
        UUID anotacaoId = UUID.randomUUID();
        Anotacao anotacao = anotacao("Texto", tag("Java"), tag("poo"));
        when(anotacaoRepository.findById(anotacaoId)).thenReturn(Optional.of(anotacao));
        participantesDaDisciplinaDaAula();
        when(anotacaoRepository.save(anotacao)).thenReturn(anotacao);
        autenticarOutroUsuario();

        Anotacao resposta = service.removerTag(anotacaoId, " java ");

        assertThat(nomesDasTags(resposta)).containsExactly("poo");
    }

    @Test
    void removerTagQueAAnotacaoNaoTemNaoMudaNada() {
        UUID anotacaoId = UUID.randomUUID();
        Anotacao anotacao = anotacao("Texto", tag("java"));
        when(anotacaoRepository.findById(anotacaoId)).thenReturn(Optional.of(anotacao));
        participantesDaDisciplinaDaAula();
        when(anotacaoRepository.save(anotacao)).thenReturn(anotacao);

        Anotacao resposta = service.removerTag(anotacaoId, "spring");

        assertThat(nomesDasTags(resposta)).containsExactly("java");
    }

    @Test
    void quemNaoParticipaNaoRemoveTags() {
        UUID anotacaoId = UUID.randomUUID();
        Anotacao anotacao = anotacao("Texto", tag("java"));
        when(anotacaoRepository.findById(anotacaoId)).thenReturn(Optional.of(anotacao));

        assertThatThrownBy(() -> service.removerTag(anotacaoId, "java"))
                .isInstanceOf(AcessoNegadoException.class);
        verify(anotacaoRepository, never()).save(any());
        assertThat(anotacao.getTags()).hasSize(1);
    }

    @Test
    void removerTagEmAnotacaoInexistenteLancaErroTratado() {
        UUID anotacaoId = UUID.randomUUID();
        when(anotacaoRepository.findById(anotacaoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removerTag(anotacaoId, "java"))
                .isInstanceOf(AnotacaoNaoEncontradaException.class);
    }

    // qualquer usuário autenticado participa da disciplina da aula (dono ou membro com participação ACEITO)
    private void participantesDaDisciplinaDaAula() {
        when(participacaoRepository.findByUsuarioIdAndGrupoId(any(), eq(disciplinaDaAulaId)))
                .thenReturn(PoliticaDeTeste.membroAceito());
    }

    @Test
    void autorEditaConteudoDaAnotacao() {
        UUID anotacaoId = UUID.randomUUID();
        Anotacao anotacao = anotacao("Texto antigo");
        when(anotacaoRepository.findById(anotacaoId)).thenReturn(Optional.of(anotacao));
        when(anotacaoRepository.save(anotacao)).thenReturn(anotacao);

        Anotacao resposta = service.editar(anotacaoId, null, "Texto revisado");

        assertThat(resposta.getConteudo()).isEqualTo("Texto revisado");
        assertThat(resposta.getAutor().getId()).isEqualTo(usuarioId);
    }

    @Test
    void autorTrocaOTituloDaAnotacao() {
        UUID anotacaoId = UUID.randomUUID();
        Anotacao anotacao = anotacao("Texto");
        anotacao.setTitulo("Antigo");
        when(anotacaoRepository.findById(anotacaoId)).thenReturn(Optional.of(anotacao));
        when(anotacaoRepository.save(anotacao)).thenReturn(anotacao);

        Anotacao resposta = service.editar(anotacaoId, "Novo", "Texto");

        assertThat(resposta.getTitulo()).isEqualTo("Novo");
        assertThat(resposta.getConteudo()).isEqualTo("Texto");
    }

    @Test
    void editarSemTituloLimpaOTitulo() {
        UUID anotacaoId = UUID.randomUUID();
        Anotacao anotacao = anotacao("Texto");
        anotacao.setTitulo("Antigo");
        when(anotacaoRepository.findById(anotacaoId)).thenReturn(Optional.of(anotacao));
        when(anotacaoRepository.save(anotacao)).thenReturn(anotacao);

        Anotacao resposta = service.editar(anotacaoId, "", "Texto");

        assertThat(resposta.getTitulo()).isNull();
    }

    @Test
    void editarDeixandoSoOTituloApagaOConteudo() {
        UUID anotacaoId = UUID.randomUUID();
        Anotacao anotacao = anotacao("Texto");
        when(anotacaoRepository.findById(anotacaoId)).thenReturn(Optional.of(anotacao));
        when(anotacaoRepository.save(anotacao)).thenReturn(anotacao);

        Anotacao resposta = service.editar(anotacaoId, "Só o título", null);

        assertThat(resposta.getTitulo()).isEqualTo("Só o título");
        assertThat(resposta.getConteudo()).isNull();
    }

    @Test
    void editarDeixandoTituloEConteudoVaziosLancaRequisicaoInvalida() {
        UUID anotacaoId = UUID.randomUUID();

        assertThatThrownBy(() -> service.editar(anotacaoId, " ", ""))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessage("Escreva um título ou um texto para a anotação.");
        verify(anotacaoRepository, never()).save(any());
    }

    @Test
    void quemNaoEhAutorNaoEditaAnotacao() {
        UUID anotacaoId = UUID.randomUUID();
        Anotacao anotacao = anotacao("Texto antigo");
        when(anotacaoRepository.findById(anotacaoId)).thenReturn(Optional.of(anotacao));
        autenticarOutroUsuario();

        assertThatThrownBy(() -> service.editar(anotacaoId, null, "Texto do colega"))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Somente o autor pode editar a anotação.");
        verify(anotacaoRepository, never()).save(any());
        assertThat(anotacao.getConteudo()).isEqualTo("Texto antigo");
    }

    @Test
    void editarAnotacaoInexistenteLancaErroTratado() {
        UUID anotacaoId = UUID.randomUUID();
        when(anotacaoRepository.findById(anotacaoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.editar(anotacaoId, null, "Texto"))
                .isInstanceOf(AnotacaoNaoEncontradaException.class);
    }

    private static void autenticarOutroUsuario() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), null, List.of()));
    }

    private void permitirAcessoA(UUID disciplinaId) {
        when(participacaoRepository.findByUsuarioIdAndGrupoId(usuarioId, disciplinaId))
            .thenReturn(PoliticaDeTeste.membroAceito());
    }

    private Participacao participacaoEm(UUID disciplinaId) {
        Disciplina disciplina = new Disciplina();
        ReflectionTestUtils.setField(disciplina, "id", disciplinaId);
        Participacao participacao = new Participacao();
        participacao.setGrupo(disciplina);
        return participacao;
    }

    @Test
    void listarPorAulaRetornaAnotacoesDaAula() {
        UUID disciplinaId = UUID.randomUUID();
        ReflectionTestUtils.setField(aula.getGrupo(), "id", disciplinaId);
        when(encontroRepository.findById(aulaId)).thenReturn(Optional.of(aula));
        permitirAcessoA(disciplinaId);
        when(anotacaoRepository.findByEncontroIdOrderByDataCriacaoAsc(aulaId))
                .thenReturn(List.of(anotacao("Primeira", tag("java")), anotacao("Segunda")));

        List<Anotacao> resposta = service.listarPorAula(aulaId);

        assertThat(resposta).extracting(Anotacao::getConteudo).containsExactly("Primeira", "Segunda");
        assertThat(nomesDasTags(resposta.get(0))).containsExactly("java");
        assertThat(resposta.get(0).getEncontro().getGrupo().getNome()).isEqualTo("PDS");
    }

    @Test
    void listarPorAulaInexistenteLancaErroTratado() {
        when(encontroRepository.findById(aulaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listarPorAula(aulaId))
                .isInstanceOf(EncontroNaoEncontradoException.class);
    }

    @Test
    void listarPorAulaSemParticipacaoAceitaLancaAcessoNegado() {
        ReflectionTestUtils.setField(aula.getGrupo(), "id", UUID.randomUUID());
        when(encontroRepository.findById(aulaId)).thenReturn(Optional.of(aula));

        assertThatThrownBy(() -> service.listarPorAula(aulaId))
                .isInstanceOf(AcessoNegadoException.class);
        verify(anotacaoRepository, never()).findByEncontroIdOrderByDataCriacaoAsc(any());
    }

    @Test
    void listarPorDisciplinaSemTagRetornaTodasAsAnotacoes() {
        UUID disciplinaId = UUID.randomUUID();
        when(grupoRepository.existsById(disciplinaId)).thenReturn(true);
        permitirAcessoA(disciplinaId);
        when(anotacaoRepository.findByEncontroGrupoIdOrderByEncontroDataAscDataCriacaoAsc(disciplinaId))
                .thenReturn(List.of(anotacao("Texto")));

        List<Anotacao> resposta = service.listarPorDisciplina(disciplinaId, null);

        assertThat(resposta).extracting(Anotacao::getConteudo).containsExactly("Texto");
    }

    @Test
    void listarPorDisciplinaComTagFiltraPelaTag() {
        UUID disciplinaId = UUID.randomUUID();
        when(grupoRepository.existsById(disciplinaId)).thenReturn(true);
        permitirAcessoA(disciplinaId);
        when(anotacaoRepository.findByEncontroGrupoIdAndTagsNomeIgnoreCaseOrderByEncontroDataAscDataCriacaoAsc(disciplinaId, "java"))
                .thenReturn(List.of(anotacao("Com tag", tag("java"))));

        List<Anotacao> resposta = service.listarPorDisciplina(disciplinaId, "  java ");

        assertThat(resposta).extracting(Anotacao::getConteudo).containsExactly("Com tag");
        verify(anotacaoRepository, never()).findByEncontroGrupoIdOrderByEncontroDataAscDataCriacaoAsc(any());
    }

    @Test
    void listarPorDisciplinaInexistenteLancaErroTratado() {
        UUID disciplinaId = UUID.randomUUID();
        when(grupoRepository.existsById(disciplinaId)).thenReturn(false);

        assertThatThrownBy(() -> service.listarPorDisciplina(disciplinaId, null))
                .isInstanceOf(GrupoNaoEncontradoException.class);
    }

    @Test
    void listarPorDisciplinaSemParticipacaoAceitaLancaAcessoNegado() {
        UUID disciplinaId = UUID.randomUUID();
        when(grupoRepository.existsById(disciplinaId)).thenReturn(true);

        assertThatThrownBy(() -> service.listarPorDisciplina(disciplinaId, null))
                .isInstanceOf(AcessoNegadoException.class);
        verify(anotacaoRepository, never()).findByEncontroGrupoIdOrderByEncontroDataAscDataCriacaoAsc(any());
    }

    @Test
    void listarPorTagBuscaSoNasDisciplinasDoUsuarioEIgnoraEspacos() {
        UUID disciplinaId = UUID.randomUUID();
        when(participacaoRepository.findByUsuarioIdAndStatus(usuarioId, StatusParticipacao.ACEITO))
                .thenReturn(List.of(participacaoEm(disciplinaId)));
        when(anotacaoRepository.findByTagsNomeIgnoreCaseAndEncontroGrupoIdInOrderByEncontroDataAscDataCriacaoAsc("java", List.of(disciplinaId)))
                .thenReturn(List.of(anotacao("Texto", tag("java"))));

        List<Anotacao> resposta = service.listarPorTag(" java ");

        assertThat(resposta).hasSize(1);
    }

    @Test
    void listarPorTagSemNenhumaParticipacaoRetornaListaVaziaSemConsultarAnotacoes() {
        when(participacaoRepository.findByUsuarioIdAndStatus(usuarioId, StatusParticipacao.ACEITO)).thenReturn(List.of());

        assertThat(service.listarPorTag("java")).isEmpty();
        verify(anotacaoRepository, never()).findByTagsNomeIgnoreCaseAndEncontroGrupoIdInOrderByEncontroDataAscDataCriacaoAsc(any(), any());
    }

    @Test
    void listarPorTagEmBrancoLancaRequisicaoInvalida() {
        assertThatThrownBy(() -> service.listarPorTag("   "))
                .isInstanceOf(RequisicaoInvalidaException.class);
    }

    @Test
    void listarDosColegasExcluiAsAnotacoesDoSolicitante() {
        UUID disciplinaId = UUID.randomUUID();
        when(grupoRepository.existsById(disciplinaId)).thenReturn(true);
        permitirAcessoA(disciplinaId);
        when(anotacaoRepository.findByEncontroGrupoIdAndAutorIdNotOrderByEncontroDataAscDataCriacaoAsc(disciplinaId, usuarioId))
                .thenReturn(List.of(anotacao("Do colega")));

        assertThat(service.listarDosColegas(disciplinaId)).extracting(Anotacao::getConteudo).containsExactly("Do colega");
    }

    @Test
    void listarDosColegasSemParticipacaoAceitaLancaAcessoNegado() {
        UUID disciplinaId = UUID.randomUUID();
        when(grupoRepository.existsById(disciplinaId)).thenReturn(true);

        assertThatThrownBy(() -> service.listarDosColegas(disciplinaId)).isInstanceOf(AcessoNegadoException.class);
    }

    private static ImagemAnotacao imagem(String chave) {
        ImagemAnotacao imagem = new ImagemAnotacao();
        imagem.setChaveArmazenamento(chave);
        return imagem;
    }

    @Test
    void autorExcluiAnotacaoEOsArquivosDasImagens() {
        UUID anotacaoId = UUID.randomUUID();
        Anotacao anotacao = anotacao("Texto", tag("java"));
        anotacao.addImagem(imagem("a.png"));
        anotacao.addImagem(imagem("b.jpg"));
        when(anotacaoRepository.findById(anotacaoId)).thenReturn(Optional.of(anotacao));

        service.excluir(anotacaoId);

        verify(anotacaoRepository).delete(anotacao);
        verify(armazenamento).remover("a.png");
        verify(armazenamento).remover("b.jpg");
    }

    @Test
    void excluirAnotacaoSemImagensNaoMexeNoArmazenamento() {
        UUID anotacaoId = UUID.randomUUID();
        Anotacao anotacao = anotacao("Texto");
        when(anotacaoRepository.findById(anotacaoId)).thenReturn(Optional.of(anotacao));

        service.excluir(anotacaoId);

        verify(anotacaoRepository).delete(anotacao);
        verify(armazenamento, never()).remover(any());
    }

    @Test
    void quemNaoEhAutorNaoExcluiAnotacao() {
        UUID anotacaoId = UUID.randomUUID();
        Anotacao anotacao = anotacao("Texto");
        anotacao.addImagem(imagem("a.png"));
        when(anotacaoRepository.findById(anotacaoId)).thenReturn(Optional.of(anotacao));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), null, List.of()));

        assertThatThrownBy(() -> service.excluir(anotacaoId))
                .isInstanceOf(AcessoNegadoException.class);
        verify(anotacaoRepository, never()).delete(any());
        verify(armazenamento, never()).remover(any());
    }

    @Test
    void excluirAnotacaoInexistenteLancaErroTratado() {
        UUID anotacaoId = UUID.randomUUID();
        when(anotacaoRepository.findById(anotacaoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir(anotacaoId))
                .isInstanceOf(AnotacaoNaoEncontradaException.class);
        verify(anotacaoRepository, never()).delete(any());
    }
}
