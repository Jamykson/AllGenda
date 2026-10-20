package com.allgenda.service.impl;

import com.allgenda.exception.AcessoNegadoException;
import com.allgenda.exception.ConflitoException;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.mapper.TagMapper;
import com.allgenda.model.Disciplina;
import com.allgenda.model.StatusParticipacao;
import com.allgenda.model.Tag;
import com.allgenda.repository.AnotacaoRepository;
import com.allgenda.repository.DisciplinaRepository;
import com.allgenda.repository.ParticipacaoRepository;
import com.allgenda.repository.TagRepository;
import com.allgenda.repository.UsuarioRepository;
import com.allgenda.service.AcessoDisciplina;
import com.allgenda.service.UsuarioAutenticado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    @Mock
    private TagRepository repository;
    @Mock
    private AnotacaoRepository anotacaoRepository;
    @Mock
    private DisciplinaRepository disciplinaRepository;
    @Mock
    private ParticipacaoRepository participacaoRepository;
    @Mock
    private UsuarioRepository usuarioRepository; // o UsuarioAutenticado só precisa dele para obter(); aqui basta o id()

    private TagServiceImpl service;

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID disciplinaId = UUID.randomUUID();
    private Disciplina disciplina;

    @BeforeEach
    void setUp() {
        UsuarioAutenticado autenticado = new UsuarioAutenticado(usuarioRepository);
        service = new TagServiceImpl(repository, anotacaoRepository, disciplinaRepository, participacaoRepository,
                autenticado, new AcessoDisciplina(participacaoRepository, autenticado), new TagMapper());
        disciplina = new Disciplina();
        ReflectionTestUtils.setField(disciplina, "id", disciplinaId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuarioId.toString(), null, List.of()));
    }

    @AfterEach
    void limparContexto() { SecurityContextHolder.clearContext(); }

    private Tag tag(String nome) {
        Tag tag = new Tag();
        tag.setNome(nome);
        tag.setDisciplina(disciplina);
        ReflectionTestUtils.setField(tag, "id", UUID.randomUUID());
        return tag;
    }

    private void participa(boolean participa) {
        when(participacaoRepository.existsByUsuarioIdAndDisciplinaIdAndStatus(usuarioId, disciplinaId, StatusParticipacao.ACEITO))
                .thenReturn(participa);
    }

    @Test
    void reaproveitaTagExistenteDaDisciplinaSemCriarNova() {
        Tag existente = tag("Java");
        when(repository.findByDisciplinaIdAndNomeIgnoreCase(disciplinaId, "java")).thenReturn(Optional.of(existente));

        Set<Tag> tags = service.buscarOuCriar(disciplina, List.of("java"));

        assertThat(tags).containsExactly(existente);
        verify(repository, never()).save(any());
    }

    @Test
    void criaTagNaDisciplinaQuandoNaoExiste() {
        when(repository.findByDisciplinaIdAndNomeIgnoreCase(disciplinaId, "POO")).thenReturn(Optional.empty());
        when(repository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        Set<Tag> tags = service.buscarOuCriar(disciplina, List.of("POO"));

        assertThat(tags).singleElement().satisfies(tag -> {
            assertThat(tag.getNome()).isEqualTo("POO");
            assertThat(tag.getDisciplina()).isSameAs(disciplina);
        });
    }

    @Test
    void nomesQueDiferemSoEmCaixaOuEspacosViramUmaTagSo() {
        when(repository.findByDisciplinaIdAndNomeIgnoreCase(eq(disciplinaId), anyString())).thenReturn(Optional.empty());
        when(repository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        Set<Tag> tags = service.buscarOuCriar(disciplina, List.of("Java", "java", " JAVA "));

        assertThat(tags).extracting(Tag::getNome).containsExactly("Java"); // mantém a primeira grafia
        verify(repository, times(1)).save(any(Tag.class));
    }

    @Test
    void ignoraNomesVaziosOuNulos() {
        Set<Tag> tags = service.buscarOuCriar(disciplina, Arrays.asList("", "   ", null));

        assertThat(tags).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void listaNulaRetornaConjuntoVazio() {
        assertThat(service.buscarOuCriar(disciplina, null)).isEmpty();
    }

    @Test
    void renomearTrocaONomeSemEspacosNasPontas() {
        Tag tag = tag("Prova");
        when(repository.findById(tag.getId())).thenReturn(Optional.of(tag));
        participa(true);
        when(repository.findByDisciplinaIdAndNomeIgnoreCase(disciplinaId, "Prova 1")).thenReturn(Optional.empty());
        when(repository.save(tag)).thenReturn(tag);

        assertThat(service.renomear(tag.getId(), "  Prova 1 ").nome()).isEqualTo("Prova 1");
    }

    @Test
    void renomearSoMaiusculasDaPropriaTagEhPermitido() {
        Tag tag = tag("prova");
        when(repository.findById(tag.getId())).thenReturn(Optional.of(tag));
        participa(true);
        when(repository.findByDisciplinaIdAndNomeIgnoreCase(disciplinaId, "Prova")).thenReturn(Optional.of(tag));
        when(repository.save(tag)).thenReturn(tag);

        assertThat(service.renomear(tag.getId(), "Prova").nome()).isEqualTo("Prova");
    }

    @Test
    void renomearParaONomeDeOutraTagDaDisciplinaLancaConflito() {
        Tag tag = tag("Prova");
        when(repository.findById(tag.getId())).thenReturn(Optional.of(tag));
        participa(true);
        when(repository.findByDisciplinaIdAndNomeIgnoreCase(disciplinaId, "resumo")).thenReturn(Optional.of(tag("Resumo")));

        assertThatThrownBy(() -> service.renomear(tag.getId(), "resumo"))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Já existe uma tag com esse nome nesta disciplina.");
        assertThat(tag.getNome()).isEqualTo("Prova");
    }

    @Test
    void renomearParaNomeVazioLancaRequisicaoInvalida() {
        assertThatThrownBy(() -> service.renomear(UUID.randomUUID(), "  "))
                .isInstanceOf(RequisicaoInvalidaException.class);
    }

    @Test
    void quemNaoParticipaNaoRenomeiaNemExclui() {
        Tag tag = tag("Prova");
        when(repository.findById(tag.getId())).thenReturn(Optional.of(tag));
        participa(false);

        assertThatThrownBy(() -> service.renomear(tag.getId(), "Outra")).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> service.excluir(tag.getId())).isInstanceOf(AcessoNegadoException.class);
        verify(repository, never()).delete(any());
    }
}
