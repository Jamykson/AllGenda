package com.allgenda.mapper;

import com.allgenda.dto.DisciplinaResponseDTO;
import com.allgenda.dto.HorarioRecorrenteResponseDTO;
import com.allgenda.model.Disciplina;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DisciplinaMapper {

    public DisciplinaResponseDTO toDto(
        Disciplina disciplina
    ) {

        if (disciplina == null) {
            return null;
        }

        HorarioRecorrenteResponseDTO horario =
            null;

        boolean possuiHorario =
            disciplina.getDias() != null &&
            !disciplina.getDias().isEmpty() &&
            disciplina.getHoraInicio() != null &&
            disciplina.getHoraFim() != null &&
            disciplina.getDataInicio() != null &&
            disciplina.getDataFim() != null;

        if (possuiHorario) {
            horario =
                new HorarioRecorrenteResponseDTO(
                    disciplina.getDias(),
                    disciplina.getHoraInicio(),
                    disciplina.getHoraFim(),
                    disciplina.getDataInicio(),
                    disciplina.getDataFim()
                );
        }

        return new DisciplinaResponseDTO(
            disciplina.getId(),
            disciplina.getNome(),
            horario
        );
    }

    public List<DisciplinaResponseDTO> toDtoList(
        List<Disciplina> disciplinas
    ) {

        return disciplinas
            .stream()
            .map(this::toDto)
            .toList();
    }
}