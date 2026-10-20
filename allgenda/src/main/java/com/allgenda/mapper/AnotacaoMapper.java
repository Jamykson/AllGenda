package com.allgenda.mapper;

import com.allgenda.dto.response.AnotacaoResponseDTO;
import com.allgenda.dto.response.ImagemAnotacaoResponseDTO;
import com.allgenda.model.Anotacao;
import com.allgenda.model.ImagemAnotacao;
import com.allgenda.model.Tag;
import org.springframework.stereotype.Component;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class AnotacaoMapper {

    private static final Comparator<Object> ORDEM_ALFABETICA = ordemAlfabetica();

    // ordem do português, sem diferenciar maiúsculas ("árvore" vem antes de "Banco")
    private static Comparator<Object> ordemAlfabetica() {
        Collator collator = Collator.getInstance(Locale.of("pt", "BR"));
        collator.setStrength(Collator.SECONDARY);
        return collator;
    }

    public AnotacaoResponseDTO toDto(Anotacao anotacao) {
        // as tags ficam num Set, sem ordem; a resposta as traz em ordem alfabética, sempre a mesma
        List<String> nomesDasTags = anotacao.getTags().stream()
                .map(Tag::getNome)
                .sorted(ORDEM_ALFABETICA)
                .toList();

        return new AnotacaoResponseDTO(
            anotacao.getId(),
            anotacao.getTitulo(),
            anotacao.getConteudo(),
            anotacao.getDataCriacao(),
            anotacao.getAutor().getId(),
            anotacao.getAutor().getNome(),
            nomesDasTags,
            anotacao.getAula().getDisciplina().getNome(),
            anotacao.getAula().getId(),
            anotacao.getAula().getData(),
            anotacao.getAula().getTopico(),
            anotacao.getImagens().stream().map(this::toImagemDto).toList()
        );
    }

    public List<AnotacaoResponseDTO> toDtoList(List<Anotacao> anotacoes) {
        return anotacoes.stream().map(this::toDto).toList();
    }

    public ImagemAnotacaoResponseDTO toImagemDto(ImagemAnotacao imagem) {
        return new ImagemAnotacaoResponseDTO(
            imagem.getId(),
            imagem.getNomeOriginal(),
            imagem.getContentType(),
            imagem.getTamanho(),
            "/api/anotacoes/" + imagem.getAnotacao().getId() + "/imagens/" + imagem.getId()
        );
    }
}
