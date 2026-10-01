package com.allgenda.service.impl;

import com.allgenda.dto.request.DisciplinaRequestDTO;
import com.allgenda.exception.DisciplinaNaoEncontradaException;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.mapper.DisciplinaMapper;
import com.allgenda.model.Disciplina;
import com.allgenda.repository.DisciplinaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisciplinaServiceImplTest {

    @Mock
    private DisciplinaRepository repository;

    private DisciplinaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DisciplinaServiceImpl(repository, new DisciplinaMapper());
    }

    @Test
    void cadastraDisciplinaComNomeEDescricaoNormalizados() {
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
    void excluiDisciplinaExistente() {
        var id = java.util.UUID.randomUUID();
        var disciplina = new Disciplina();
        when(repository.findById(id)).thenReturn(java.util.Optional.of(disciplina));

        service.excluir(id);

        verify(repository).delete(disciplina);
    }

    @Test
    void rejeitaExclusaoDeDisciplinaInexistente() {
        var id = java.util.UUID.randomUUID();
        when(repository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.excluir(id))
            .isInstanceOf(DisciplinaNaoEncontradaException.class);

        verify(repository, never()).delete(any(Disciplina.class));
    }
}