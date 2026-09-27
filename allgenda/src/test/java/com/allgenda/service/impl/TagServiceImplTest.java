package com.allgenda.service.impl;

import com.allgenda.mapper.TagMapper;
import com.allgenda.model.Tag;
import com.allgenda.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    @Mock
    private TagRepository repository;

    private TagServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TagServiceImpl(repository, new TagMapper());
    }

    private static Tag tag(String nome) {
        Tag tag = new Tag();
        tag.setNome(nome);
        return tag;
    }

    @Test
    void reaproveitaTagExistenteSemCriarNova() {
        Tag existente = tag("Java");
        when(repository.findByNomeIgnoreCase("java")).thenReturn(Optional.of(existente));

        Set<Tag> tags = service.buscarOuCriar(List.of("java"));

        assertThat(tags).containsExactly(existente);
        verify(repository, never()).save(any());
    }

    @Test
    void criaTagQuandoNaoExiste() {
        when(repository.findByNomeIgnoreCase("POO")).thenReturn(Optional.empty());
        when(repository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        Set<Tag> tags = service.buscarOuCriar(List.of("POO"));

        assertThat(tags).extracting(Tag::getNome).containsExactly("POO");
        verify(repository).save(any(Tag.class));
    }

    @Test
    void nomesQueDiferemSoEmCaixaOuEspacosViramUmaTagSo() {
        when(repository.findByNomeIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        Set<Tag> tags = service.buscarOuCriar(List.of("Java", "java", " JAVA "));

        assertThat(tags).extracting(Tag::getNome).containsExactly("Java"); // mantém a primeira grafia
        verify(repository, times(1)).save(any(Tag.class));
    }

    @Test
    void ignoraNomesVaziosOuNulos() {
        Set<Tag> tags = service.buscarOuCriar(Arrays.asList("", "   ", null));

        assertThat(tags).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void listaNulaRetornaConjuntoVazio() {
        assertThat(service.buscarOuCriar(null)).isEmpty();
    }
}
