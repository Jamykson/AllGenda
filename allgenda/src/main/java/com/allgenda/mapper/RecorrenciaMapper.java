package com.allgenda.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.allgenda.dto.response.RecorrenciaResponseDTO;
import com.allgenda.model.Recorrencia;

@Component
public class RecorrenciaMapper {

    public RecorrenciaResponseDTO toDto(Recorrencia recorrencia) {
        return new RecorrenciaResponseDTO(
            recorrencia.getGrupo().getId(),
            recorrencia.getDiasSemana().stream().sorted().toList(),   // domingo a sábado
            recorrencia.getHoraInicio(),
            recorrencia.getHoraFim(),
            recorrencia.getDataInicio(),
            recorrencia.getDataFim(),
            recorrencia.getFrequencia()
        );
    }

    public List<RecorrenciaResponseDTO> toDtoList(List<Recorrencia> recorrencias) {
        return recorrencias.stream().map(this::toDto).toList();
    }
}
