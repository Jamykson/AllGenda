package com.allgenda.mapper

import com.allgenda.dto.response.AulaResponseDTO;
import com.allgenda.model.Aula;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AulaMapper {

    public AulaResponseDTO toDto(Aula aula){
        return new AulaResponseDTO(
            aula.getId(),
            aula.getData(),
            aula.getHorario(),
            aula.getTopico(),
            aula.getDisciplina().getNome()
        );
    }

    public List<AulaResponseDTO> toDtoList(List<Aula> aulas) {
        return aulas.stream().map(this::toDto).toList();
    }
}