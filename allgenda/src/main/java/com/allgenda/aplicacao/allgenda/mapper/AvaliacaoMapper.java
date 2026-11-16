package com.allgenda.aplicacao.allgenda.mapper;

import com.allgenda.aplicacao.allgenda.dto.response.AvaliacaoResponseDTO;
import com.allgenda.aplicacao.allgenda.model.Avaliacao;
import com.allgenda.framework.model.Evento;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class AvaliacaoMapper {

    // no AllGenda todo evento é uma Avaliacao (o único tipo de evento registrado)
    public AvaliacaoResponseDTO toDto(Evento evento) {
        Avaliacao avaliacao = (Avaliacao) evento;
        return new AvaliacaoResponseDTO(
                avaliacao.getId(),
                avaliacao.getDescricao(),
                avaliacao.getData(),
                avaliacao.getTipo(),
                avaliacao.getGrupo().getId(),
                avaliacao.getGrupo().getNome());
    }

    public List<AvaliacaoResponseDTO> toDtoList(List<Evento> avaliacoes) {
        return avaliacoes.stream().map(this::toDto).toList();
    }
}
