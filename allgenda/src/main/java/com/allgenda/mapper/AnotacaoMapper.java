package com.allgenda.mapper;

import com.allgenda.dto.response.AnotacaoResponseDTO;
import com.allgenda.model.Anotacao;
import com.allgenda.mode.Tag;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class AnotacaoMapper {

    public AnotacaoResponseDTO toDto(Anotacao anotacao) {
        List<String> nomesDasTags = anotacao.getTags().stream()
                .map(Tag::getNome)
                .toList();

        return new AnotacaoResponseDTO(
            anotacao.getId(),
            anotacao.getConteudo(),
            anotacao.getAutor().getNome(),
            nomesDasTags
            anotacao.getAula().getDisciplina().getNome()
        );
    }

    public List<AnotacaoResponseDTO> toDtoList(List<Anotacao> anotacoes) {
        return anotacoes.stream().map(this::toDto).toList();
    }
}