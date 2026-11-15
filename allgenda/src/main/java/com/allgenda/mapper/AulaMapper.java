package com.allgenda.mapper;

import com.allgenda.dto.response.AulaResponseDTO;
import com.allgenda.model.Aula;
import com.allgenda.model.VisibilidadeAnotacao;
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
            aula.getDisciplina().getId(),
            aula.getDisciplina().getNome(),
            aula.isCancelada(),
            aula.getAnotacoes().stream().filter(a -> a.getVisibilidade() == VisibilidadeAnotacao.GRUPO).count()
        );
    }

    public List<AulaResponseDTO> toDtoList(List<Aula> aulas) {
        return aulas.stream().map(this::toDto).toList();
    }
}