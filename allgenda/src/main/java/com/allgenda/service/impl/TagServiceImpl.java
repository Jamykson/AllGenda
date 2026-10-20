package com.allgenda.service.impl;

import com.allgenda.dto.response.TagResponseDTO;
import com.allgenda.exception.AcessoNegadoException;
import com.allgenda.exception.ConflitoException;
import com.allgenda.exception.DisciplinaNaoEncontradaException;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.exception.TagNaoEncontradaException;
import com.allgenda.mapper.TagMapper;
import com.allgenda.model.Disciplina;
import com.allgenda.model.StatusParticipacao;
import com.allgenda.model.Tag;
import com.allgenda.repository.AnotacaoRepository;
import com.allgenda.repository.DisciplinaRepository;
import com.allgenda.repository.ParticipacaoRepository;
import com.allgenda.repository.TagRepository;
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

    private final TagRepository repository;
    private final AnotacaoRepository anotacaoRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final ParticipacaoRepository participacaoRepository;
    private final UsuarioAutenticado usuarioAutenticado;
    private final TagMapper mapper;

    public TagServiceImpl(TagRepository repository,
                          AnotacaoRepository anotacaoRepository,
                          DisciplinaRepository disciplinaRepository,
                          ParticipacaoRepository participacaoRepository,
                          UsuarioAutenticado usuarioAutenticado,
                          TagMapper mapper) {
        this.repository = repository;
        this.anotacaoRepository = anotacaoRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.participacaoRepository = participacaoRepository;
        this.usuarioAutenticado = usuarioAutenticado;
        this.mapper = mapper;
    }

    /*/ Para cada nome recebido, reaproveita a tag da disciplina que já existe ou cria uma nova nela.
        Nomes são comparados sem diferenciar maiúsculas/minúsculas e sem espaços nas pontas:
        "Java", "java" e " JAVA " resultam na mesma tag, que mantém a grafia de quando foi criada. /*/
    @Override
    @Transactional
    public Set<Tag> buscarOuCriar(Disciplina disciplina, List<String> nomes) {
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
            Tag tag = repository.findByDisciplinaIdAndNomeIgnoreCase(disciplina.getId(), nome)
                .orElseGet(() -> {
                    Tag nova = new Tag();
                    nova.setNome(nome);
                    nova.setDisciplina(disciplina);
                    return repository.save(nova);
                });
            tags.add(tag);
        }
        return tags;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponseDTO> listar(UUID disciplinaId) {
        List<UUID> disciplinas;
        if (disciplinaId != null) {
            if (!disciplinaRepository.existsById(disciplinaId)) {
                throw new DisciplinaNaoEncontradaException(disciplinaId);
            }
            exigirParticipanteAceito(disciplinaId);
            disciplinas = List.of(disciplinaId);
        }
        else {
            disciplinas = participacaoRepository
                .findByUsuarioIdAndStatus(usuarioAutenticado.id(), StatusParticipacao.ACEITO)
                .stream()
                .map(participacao -> participacao.getDisciplina().getId())
                .toList();
            if (disciplinas.isEmpty()) {
                return List.of();
            }
        }

        // ordem alfabética do português, sem diferenciar maiúsculas ("árvore" vem antes de "Banco")
        Collator collator = Collator.getInstance(Locale.of("pt", "BR"));
        collator.setStrength(Collator.SECONDARY);
        return mapper.toDtoList(repository.findByDisciplinaIdIn(disciplinas).stream()
            .sorted(Comparator.comparing(Tag::getNome, collator))
            .toList());
    }

    @Override
    @Transactional
    public TagResponseDTO renomear(UUID tagId, String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RequisicaoInvalidaException("O nome da tag é obrigatório.");
        }
        String novoNome = nome.trim();
        validarTamanho(novoNome);

        Tag tag = buscarTagDeParticipante(tagId);
        // mudar só maiúsculas e minúsculas da própria tag é permitido; usar o nome de outra, não
        repository.findByDisciplinaIdAndNomeIgnoreCase(tag.getDisciplina().getId(), novoNome)
            .filter(outra -> !outra.getId().equals(tag.getId()))
            .ifPresent(outra -> {
                throw new ConflitoException("Já existe uma tag com esse nome nesta disciplina.");
            });

        tag.setNome(novoNome);
        return mapper.toDto(repository.save(tag));
    }

    @Override
    @Transactional
    public void excluir(UUID tagId) {
        Tag tag = buscarTagDeParticipante(tagId);
        // o vínculo com cada anotação sai antes; as anotações continuam existindo
        anotacaoRepository.findByTagsId(tagId).forEach(anotacao -> anotacao.removeTag(tag));
        tag.getDisciplina().getTags().remove(tag);
        repository.delete(tag);
    }

    private Tag buscarTagDeParticipante(UUID tagId) {
        Tag tag = repository.findById(tagId)
            .orElseThrow(() -> new TagNaoEncontradaException(tagId));
        // uma tag antiga, ainda sem disciplina, não pertence a ninguém: só a migração mexe nela
        if (tag.getDisciplina() == null) {
            throw new TagNaoEncontradaException(tagId);
        }
        exigirParticipanteAceito(tag.getDisciplina().getId());
        return tag;
    }

    // dono e membro têm participação ACEITO; sem participação, pendente ou recusada: sem acesso
    private void exigirParticipanteAceito(UUID disciplinaId) {
        boolean participa = participacaoRepository.existsByUsuarioIdAndDisciplinaIdAndStatus(
            usuarioAutenticado.id(), disciplinaId, StatusParticipacao.ACEITO);
        if (!participa) {
            throw new AcessoNegadoException("Somente participantes da disciplina gerenciam as tags dela.");
        }
    }

    private static void validarTamanho(String nome) {
        if (nome.length() > TAMANHO_MAXIMO_NOME) {
            throw new RequisicaoInvalidaException("O nome da tag deve ter no máximo " + TAMANHO_MAXIMO_NOME + " caracteres.");
        }
    }
}
