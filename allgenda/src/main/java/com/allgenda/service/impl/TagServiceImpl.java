package com.allgenda.service.impl;

import com.allgenda.dto.response.TagResponseDTO;
import com.allgenda.mapper.TagMapper;
import com.allgenda.model.Tag;
import com.allgenda.repository.TagRepository;
import com.allgenda.service.TagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class TagServiceImpl implements TagService {

    private final TagRepository repository;
    private final TagMapper mapper;

    public TagServiceImpl(TagRepository repository, TagMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /*/ Para cada nome recebido, reaproveita a tag já existente ou cria uma nova.
        Nomes são comparados sem diferenciar maiúsculas/minúsculas e sem espaços nas pontas:
        "Java", "java" e " JAVA " resultam na mesma tag, que mantém a grafia de quando foi criada. /*/
    @Override
    @Transactional
    public Set<Tag> buscarOuCriar(List<String> nomes) {
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
            Tag tag = repository.findByNomeIgnoreCase(nome)
                .orElseGet(() -> {
                    Tag nova = new Tag();
                    nova.setNome(nome);
                    return repository.save(nova);
                });
            tags.add(tag);
        }
        return tags;
    }

    @Override
    public List<TagResponseDTO> listarTodas() {
        return mapper.toDtoList(repository.findAll());
    }
}
