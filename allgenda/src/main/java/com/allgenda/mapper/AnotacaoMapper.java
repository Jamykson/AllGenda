package com.allgenda.mapper;

import com.allgenda.dto.response.AnotacaoResponseDTO;
import com.allgenda.model.Anotacao;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class AnotacaoMapper {

    public AnotacaoResponseDTO toDto(Anotacao anotacao) {
        return new AnotacaoResponseDTO(
            anotacao.getId(),
            anotacao.getConteudo(),
            anotacao.getAutor().getNome()
        );
    }

    public List<AnotacaoResponseDTO> toDtoList(List<Anotacao> anotacoes) {
        return anotacoes.stream().map(this::toDto).toList();
    }
}