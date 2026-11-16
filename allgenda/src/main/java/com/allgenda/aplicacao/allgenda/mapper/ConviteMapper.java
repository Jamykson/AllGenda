package com.allgenda.aplicacao.allgenda.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.allgenda.aplicacao.allgenda.dto.response.ConviteResponseDTO;
import com.allgenda.framework.model.Participacao;

@Component
public class ConviteMapper {

    public ConviteResponseDTO toDto(Participacao participacao) {
        return new ConviteResponseDTO(
            participacao.getId(),
            participacao.getGrupo().getId(),
            participacao.getGrupo().getNome(),
            participacao.getUsuario().getId(),
            participacao.getUsuario().getNome(),
            participacao.getPapel().codigo(),
            participacao.getStatus()
        );
    }

    public List<ConviteResponseDTO> toDtoList(List<Participacao> participacoes) {
        return participacoes.stream().map(this::toDto).toList();
    }
}

