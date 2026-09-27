package com.allgenda.service.impl;

import com.allgenda.config.UsuarioPadraoInitializer;
import com.allgenda.dto.request.AnotacaoRequestDTO;
import com.allgenda.dto.response.AnotacaoResponseDTO;
import com.allgenda.exception.AnotacaoNaoEncontradaException;
import com.allgenda.exception.AulaNaoEncontradaException;
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
    private UsuarioRepository usuarioRepository;
    @Mock
    private TagService tagService;

    private AnotacaoServiceImpl service;

    private final UUID aulaId = UUID.randomUUID();
    private Aula aula;
    private Usuario usuarioPadrao;

    @BeforeEach
    void setUp() {
        service = new AnotacaoServiceImpl(anotacaoRepository, aulaRepository, usuarioRepository, tagService, new AnotacaoMapper());

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
}
