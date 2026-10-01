package com.allgenda.mapper;

import com.allgenda.dto.response.DisciplinaResponseDTO;
import com.allgenda.model.Disciplina;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DisciplinaMapper {

    public DisciplinaResponseDTO toDto(Disciplina disciplina){
        return new DisciplinaResponseDTO(
            disciplina.getId(),
            disciplina.getNome(),
            disciplina.getDescricao()
        );
    }

    public List<DisciplinaResponseDTO> toDtoList(List<Disciplina> disciplinas) {
        return disciplinas.stream().map(this::toDto).toList();
    }
}