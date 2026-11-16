package com.allgenda.mapper;

import com.allgenda.dto.response.DisciplinaResponseDTO;
import com.allgenda.model.Grupo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DisciplinaMapper {

    public DisciplinaResponseDTO toDto(Grupo disciplina){
        return new DisciplinaResponseDTO(
            disciplina.getId(),
            disciplina.getNome(),
            disciplina.getDescricao()
        );
    }

    public List<DisciplinaResponseDTO> toDtoList(List<Grupo> disciplinas) {
        return disciplinas.stream().map(this::toDto).toList();
    }
}