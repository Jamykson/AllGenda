package com.allgenda.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.allgenda.dto.response.ConviteResponseDTO;
import com.allgenda.model.Participacao;

@Component
public class ConviteMapper {

    public ConviteResponseDTO toDto(Participacao participacao) {
        return new ConviteResponseDTO(
            participacao.getId(),
            participacao.getDisciplina().getId(),
            participacao.getDisciplina().getNome(),
            participacao.getUsuario().getId(),
            participacao.getUsuario().getNome(),
            participacao.getPapel(),
            participacao.getStatus()
        );
    }

    public List<ConviteResponseDTO> toDtoList(List<Participacao> participacoes) {
        return participacoes.stream().map(this::toDto).toList();
    }
}

