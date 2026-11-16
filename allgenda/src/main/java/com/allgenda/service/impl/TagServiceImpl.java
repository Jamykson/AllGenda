package com.allgenda.service.impl;

import com.allgenda.exception.ConflitoException;
import com.allgenda.exception.GrupoNaoEncontradoException;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.exception.TagNaoEncontradaException;
import com.allgenda.model.Grupo;
import com.allgenda.model.StatusParticipacao;
import com.allgenda.model.Tag;
import com.allgenda.repository.AnotacaoRepository;
import com.allgenda.repository.GrupoRepository;
import com.allgenda.repository.ParticipacaoRepository;
import com.allgenda.repository.TagRepository;
import com.allgenda.service.AcessoDisciplina;
import com.allgenda.service.TagService;
import com.allgenda.service.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class TagServiceImpl implements TagService {

    private static final int TAMANHO_MAXIMO_NOME = 255;
    private static final String SEM_ACESSO = "Somente participantes da disciplina gerenciam as tags dela.";

    private final TagRepository repository;
    private final AnotacaoRepository anotacaoRepository;
    private final GrupoRepository grupoRepository;
    private final ParticipacaoRepository participacaoRepository;
    private final UsuarioAutenticado usuarioAutenticado;
    private final AcessoDisciplina acesso;

    public TagServiceImpl(TagRepository repository,
                          AnotacaoRepository anotacaoRepository,
                          GrupoRepository grupoRepository,
                          ParticipacaoRepository participacaoRepository,
                          UsuarioAutenticado usuarioAutenticado,
                          AcessoDisciplina acesso) {
        this.repository = repository;
        this.anotacaoRepository = anotacaoRepository;
        this.grupoRepository = grupoRepository;
        this.participacaoRepository = participacaoRepository;
        this.usuarioAutenticado = usuarioAutenticado;
        this.acesso = acesso;
    }

    /*/ Para cada nome recebido, reaproveita a tag da disciplina que já existe ou cria uma nova nela.
        Nomes são comparados sem diferenciar maiúsculas/minúsculas e sem espaços nas pontas:
        "Java", "java" e " JAVA " resultam na mesma tag, que mantém a grafia de quando foi criada. /*/
    @Override
    @Transactional
    public Set<Tag> buscarOuCriar(Grupo disciplina, List<String> nomes) {
        Set<Tag> tags = new LinkedHashSet<>();
        if (nomes == null) {
            return tags;
        }

        // remove nomes vazios e repetidos dentro da própria requisição, preservando a ordem e a primeira grafia
        Map<String, String> nomesUnicos = new LinkedHashMap<>();
        for (String nome : nomes) {
            if (nome == null || nome.isBlank()) {
                continue;
            }
            String limpo = nome.trim();
            nomesUnicos.putIfAbsent(limpo.toLowerCase(Locale.ROOT), limpo);
        }

        for (String nome : nomesUnicos.values()) {
            validarTamanho(nome);
            Tag tag = repository.findByGrupoIdAndNomeIgnoreCase(disciplina.getId(), nome)
                .orElseGet(() -> {
                    Tag nova = new Tag();
                    nova.setNome(nome);
                    nova.setGrupo(disciplina);
                    return repository.save(nova);
                });
            tags.add(tag);
        }
        return tags;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> listar(UUID disciplinaId) {
        List<UUID> disciplinas;
        if (disciplinaId != null) {
            if (!grupoRepository.existsById(disciplinaId)) {
                throw new GrupoNaoEncontradoException(disciplinaId);
            }
            acesso.exigirParticipanteAceito(disciplinaId, SEM_ACESSO);
            disciplinas = List.of(disciplinaId);
        }
        else {
            disciplinas = participacaoRepository
                .findByUsuarioIdAndStatus(usuarioAutenticado.id(), StatusParticipacao.ACEITO)
                .stream()
                .map(participacao -> participacao.getGrupo().getId())
                .toList();
            if (disciplinas.isEmpty()) {
                return List.of();
            }
        }

        // ordem alfabética do português, sem diferenciar maiúsculas ("árvore" vem antes de "Banco")
        Collator collator = Collator.getInstance(Locale.of("pt", "BR"));
        collator.setStrength(Collator.SECONDARY);
        return repository.findByGrupoIdIn(disciplinas).stream()
            .sorted(Comparator.comparing(Tag::getNome, collator))
            .toList();
    }

    @Override
    @Transactional
    public Tag renomear(UUID tagId, String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RequisicaoInvalidaException("O nome da tag é obrigatório.");
        }
        String novoNome = nome.trim();
        validarTamanho(novoNome);

        Tag tag = buscarTagDeParticipante(tagId);
        // mudar só maiúsculas e minúsculas da própria tag é permitido; usar o nome de outra, não
        repository.findByGrupoIdAndNomeIgnoreCase(tag.getGrupo().getId(), novoNome)
            .filter(outra -> !outra.getId().equals(tag.getId()))
            .ifPresent(outra -> {
                throw new ConflitoException("Já existe uma tag com esse nome nesta disciplina.");
            });

        tag.setNome(novoNome);
        return repository.save(tag);
    }

    @Override
    @Transactional
    public void excluir(UUID tagId) {
        Tag tag = buscarTagDeParticipante(tagId);
        // o vínculo com cada anotação sai antes; as anotações continuam existindo
        anotacaoRepository.findByTagsId(tagId).forEach(anotacao -> anotacao.removeTag(tag));
        tag.getGrupo().getTags().remove(tag);
        repository.delete(tag);
    }

    private Tag buscarTagDeParticipante(UUID tagId) {
        Tag tag = repository.findById(tagId)
            .orElseThrow(() -> new TagNaoEncontradaException(tagId));
        acesso.exigirParticipanteAceito(tag.getGrupo().getId(), SEM_ACESSO);
        return tag;
    }


    private static void validarTamanho(String nome) {
        if (nome.length() > TAMANHO_MAXIMO_NOME) {
            throw new RequisicaoInvalidaException("O nome da tag deve ter no máximo " + TAMANHO_MAXIMO_NOME + " caracteres.");
        }
    }
}
