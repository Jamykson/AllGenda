package com.allgenda.mapper;

import com.allgenda.dto.response.AvaliacaoResponseDTO;
import com.allgenda.model.Avaliacao;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class AvaliacaoMapper {

    public AvaliacaoResponseDTO toDto(Avaliacao avaliacao) {
        return new AvaliacaoResponseDTO(
                avaliacao.getId(),
                avaliacao.getDescricao(),
                avaliacao.getData(),
                avaliacao.getTipo(),
                avaliacao.getDisciplina().getId(),
                avaliacao.getDisciplina().getNome());
    }

    public List<AvaliacaoResponseDTO> toDtoList(List<Avaliacao> avaliacoes) {
        return avaliacoes.stream().map(this::toDto).toList();
    }
}
