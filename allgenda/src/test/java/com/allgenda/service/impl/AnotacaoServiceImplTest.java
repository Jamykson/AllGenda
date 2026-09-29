package com.allgenda.service.impl;

import com.allgenda.config.UsuarioPadraoInitializer;
import com.allgenda.dto.request.AnotacaoRequestDTO;
import com.allgenda.dto.response.AnotacaoResponseDTO;
import com.allgenda.exception.AnotacaoNaoEncontradaException;
import com.allgenda.exception.AulaNaoEncontradaException;
import com.allgenda.exception.DisciplinaNaoEncontradaException;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.exception.UsuarioNaoEncontradoException;
import com.allgenda.mapper.AnotacaoMapper;
import com.allgenda.model.Anotacao;
import com.allgenda.model.Aula;
import com.allgenda.model.Disciplina;
import com.allgenda.model.Tag;
import com.allgenda.model.Usuario;
import com.allgenda.repository.AnotacaoRepository;
import com.allgenda.repository.AulaRepository;
import com.allgenda.repository.DisciplinaRepository;
import com.allgenda.repository.UsuarioRepository;
import com.allgenda.service.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnotacaoServiceImplTest {

    @Mock
    private AnotacaoRepository anotacaoRepository;
    @Mock
    private AulaRepository aulaRepository;
    @Mock
    private DisciplinaRepository disciplinaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TagService tagService;

    private AnotacaoServiceImpl service;

    private final UUID aulaId = UUID.randomUUID();
    private Aula aula;
    private Usuario usuarioPadrao;

    @BeforeEach
    void setUp() {
        service = new AnotacaoServiceImpl(anotacaoRepository, aulaRepository, disciplinaRepository, usuarioRepository, tagService, new AnotacaoMapper());

        Disciplina disciplina = new Disciplina();
        disciplina.setNome("PDS");
        aula = new Aula();
        aula.setTopico("Padrões de projeto");
        aula.setDisciplina(disciplina);

        usuarioPadrao = new Usuario();
        usuarioPadrao.setNome(UsuarioPadraoInitializer.NOME_USUARIO_PADRAO);
    }

    private static Tag tag(String nome) {
        Tag tag = new Tag();
        tag.setNome(nome);
        return tag;
    }

    private Anotacao anotacao(String conteudo, Tag... tags) {
        Anotacao anotacao = new Anotacao();
        anotacao.setConteudo(conteudo);
        anotacao.setAula(aula);
        anotacao.setAutor(usuarioPadrao);
        for (Tag tag : tags) {
            anotacao.addTag(tag);
        }
        return anotacao;
    }

    @Test
    void cadastraAnotacaoEmAulaExistenteComUsuarioPadraoETags() {
        when(aulaRepository.findById(aulaId)).thenReturn(Optional.of(aula));
        when(usuarioRepository.findByNome(UsuarioPadraoInitializer.NOME_USUARIO_PADRAO)).thenReturn(Optional.of(usuarioPadrao));
        when(tagService.buscarOuCriar(List.of("java", "poo"))).thenReturn(new LinkedHashSet<>(List.of(tag("java"), tag("poo"))));
        when(anotacaoRepository.save(any(Anotacao.class))).thenAnswer(inv -> inv.getArgument(0));

        AnotacaoResponseDTO resposta = service.cadastrar(new AnotacaoRequestDTO(aulaId, null, "Strategy e Observer", List.of("java", "poo")));

        assertThat(resposta.conteudo()).isEqualTo("Strategy e Observer");
        assertThat(resposta.autorNome()).isEqualTo(UsuarioPadraoInitializer.NOME_USUARIO_PADRAO);
        assertThat(resposta.aulaTopico()).isEqualTo("Padrões de projeto");
        assertThat(resposta.disciplinaNome()).isEqualTo("PDS");
        assertThat(resposta.tags()).containsExactlyInAnyOrder("java", "poo");
    }

    @Test
    void cadastraAnotacaoSemTags() {
        when(aulaRepository.findById(aulaId)).thenReturn(Optional.of(aula));
        when(usuarioRepository.findByNome(UsuarioPadraoInitializer.NOME_USUARIO_PADRAO)).thenReturn(Optional.of(usuarioPadrao));
        when(tagService.buscarOuCriar(null)).thenReturn(Set.of());
        when(anotacaoRepository.save(any(Anotacao.class))).thenAnswer(inv -> inv.getArgument(0));

        AnotacaoResponseDTO resposta = service.cadastrar(new AnotacaoRequestDTO(aulaId, null, "Sem tags", null));

        assertThat(resposta.tags()).isEmpty();
    }

    @Test
    void usaAutorInformadoQuandoExiste() {
        UUID autorId = UUID.randomUUID();
        Usuario autor = new Usuario();
        autor.setNome("Maria");
        when(aulaRepository.findById(aulaId)).thenReturn(Optional.of(aula));
        when(usuarioRepository.findById(autorId)).thenReturn(Optional.of(autor));
        when(tagService.buscarOuCriar(null)).thenReturn(Set.of());
        when(anotacaoRepository.save(any(Anotacao.class))).thenAnswer(inv -> inv.getArgument(0));

        AnotacaoResponseDTO resposta = service.cadastrar(new AnotacaoRequestDTO(aulaId, autorId, "Texto", null));

        assertThat(resposta.autorNome()).isEqualTo("Maria");
    }

    @Test
    void autorInexistenteLancaErroTratado() {
        UUID autorId = UUID.randomUUID();
        when(aulaRepository.findById(aulaId)).thenReturn(Optional.of(aula));
        when(usuarioRepository.findById(autorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cadastrar(new AnotacaoRequestDTO(aulaId, autorId, "Texto", null)))
                .isInstanceOf(UsuarioNaoEncontradoException.class);
        verify(anotacaoRepository, never()).save(any());
    }

    @Test
    void aulaInexistenteLancaErroTratado() {
        when(aulaRepository.findById(aulaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cadastrar(new AnotacaoRequestDTO(aulaId, null, "Texto", null)))
                .isInstanceOf(AulaNaoEncontradaException.class);
        verify(anotacaoRepository, never()).save(any());
    }

    @Test
    void conteudoVazioLancaRequisicaoInvalida() {
        assertThatThrownBy(() -> service.cadastrar(new AnotacaoRequestDTO(aulaId, null, "   ", null)))
                .isInstanceOf(RequisicaoInvalidaException.class);
        verify(anotacaoRepository, never()).save(any());
    }

    @Test
    void aulaIdNuloLancaRequisicaoInvalida() {
        assertThatThrownBy(() -> service.cadastrar(new AnotacaoRequestDTO(null, null, "Texto", null)))
                .isInstanceOf(RequisicaoInvalidaException.class);
    }

    @Test
    void associarTagsAcrescentaAsTagsExistentes() {
        UUID anotacaoId = UUID.randomUUID();
        Anotacao anotacao = new Anotacao();
        anotacao.setConteudo("Texto");
        anotacao.setAula(aula);
        anotacao.setAutor(usuarioPadrao);
        anotacao.addTag(tag("java"));
        when(anotacaoRepository.findById(anotacaoId)).thenReturn(Optional.of(anotacao));
        when(tagService.buscarOuCriar(List.of("poo", "spring"))).thenReturn(new LinkedHashSet<>(List.of(tag("poo"), tag("spring"))));
        when(anotacaoRepository.save(anotacao)).thenReturn(anotacao);

        AnotacaoResponseDTO resposta = service.associarTags(anotacaoId, List.of("poo", "spring"));

        assertThat(resposta.tags()).containsExactlyInAnyOrder("java", "poo", "spring");
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
    void listarPorAulaRetornaAnotacoesDaAula() {
        when(aulaRepository.existsById(aulaId)).thenReturn(true);
        when(anotacaoRepository.findByAulaIdOrderByDataCriacaoAsc(aulaId))
                .thenReturn(List.of(anotacao("Primeira", tag("java")), anotacao("Segunda")));

        List<AnotacaoResponseDTO> resposta = service.listarPorAula(aulaId);

        assertThat(resposta).extracting(AnotacaoResponseDTO::conteudo).containsExactly("Primeira", "Segunda");
        assertThat(resposta.get(0).tags()).containsExactly("java");
        assertThat(resposta.get(0).disciplinaNome()).isEqualTo("PDS");
    }

    @Test
    void listarPorAulaInexistenteLancaErroTratado() {
        when(aulaRepository.existsById(aulaId)).thenReturn(false);

        assertThatThrownBy(() -> service.listarPorAula(aulaId))
                .isInstanceOf(AulaNaoEncontradaException.class);
    }

    @Test
    void listarPorDisciplinaSemTagRetornaTodasAsAnotacoes() {
        UUID disciplinaId = UUID.randomUUID();
        when(disciplinaRepository.existsById(disciplinaId)).thenReturn(true);
        when(anotacaoRepository.findByAulaDisciplinaIdOrderByAulaDataAscDataCriacaoAsc(disciplinaId))
                .thenReturn(List.of(anotacao("Texto")));

        List<AnotacaoResponseDTO> resposta = service.listarPorDisciplina(disciplinaId, null);

        assertThat(resposta).extracting(AnotacaoResponseDTO::conteudo).containsExactly("Texto");
    }

    @Test
    void listarPorDisciplinaComTagFiltraPelaTag() {
        UUID disciplinaId = UUID.randomUUID();
        when(disciplinaRepository.existsById(disciplinaId)).thenReturn(true);
        when(anotacaoRepository.findByAulaDisciplinaIdAndTagsNomeIgnoreCaseOrderByAulaDataAscDataCriacaoAsc(disciplinaId, "java"))
                .thenReturn(List.of(anotacao("Com tag", tag("java"))));

        List<AnotacaoResponseDTO> resposta = service.listarPorDisciplina(disciplinaId, "  java ");

        assertThat(resposta).extracting(AnotacaoResponseDTO::conteudo).containsExactly("Com tag");
        verify(anotacaoRepository, never()).findByAulaDisciplinaIdOrderByAulaDataAscDataCriacaoAsc(any());
    }

    @Test
    void listarPorDisciplinaInexistenteLancaErroTratado() {
        UUID disciplinaId = UUID.randomUUID();
        when(disciplinaRepository.existsById(disciplinaId)).thenReturn(false);

        assertThatThrownBy(() -> service.listarPorDisciplina(disciplinaId, null))
                .isInstanceOf(DisciplinaNaoEncontradaException.class);
    }

    @Test
    void listarPorTagIgnoraEspacosEmVolta() {
        when(anotacaoRepository.findByTagsNomeIgnoreCaseOrderByAulaDataAscDataCriacaoAsc("java"))
                .thenReturn(List.of(anotacao("Texto", tag("java"))));

        List<AnotacaoResponseDTO> resposta = service.listarPorTag(" java ");

        assertThat(resposta).hasSize(1);
    }

    @Test
    void listarPorTagInexistenteRetornaListaVazia() {
        when(anotacaoRepository.findByTagsNomeIgnoreCaseOrderByAulaDataAscDataCriacaoAsc("nada")).thenReturn(List.of());

        assertThat(service.listarPorTag("nada")).isEmpty();
    }

    @Test
    void listarPorTagEmBrancoLancaRequisicaoInvalida() {
        assertThatThrownBy(() -> service.listarPorTag("   "))
                .isInstanceOf(RequisicaoInvalidaException.class);
    }
}
