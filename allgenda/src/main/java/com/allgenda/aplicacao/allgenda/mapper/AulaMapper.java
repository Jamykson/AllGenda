package com.allgenda.aplicacao.allgenda.mapper;

import com.allgenda.aplicacao.allgenda.dto.response.AulaResponseDTO;
import com.allgenda.framework.model.Encontro;
import com.allgenda.framework.model.VisibilidadeAnotacao;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AulaMapper {

    public AulaResponseDTO toDto(Encontro aula){
        return new AulaResponseDTO(
            aula.getId(),
            aula.getData(),
            aula.getHorario(),
            aula.getTopico(),
            aula.getGrupo().getId(),
            aula.getGrupo().getNome(),
            aula.isCancelada(),
            aula.getAnotacoes().stream().filter(a -> a.getVisibilidade() == VisibilidadeAnotacao.GRUPO).count()
        );
    }

    public List<AulaResponseDTO> toDtoList(List<Encontro> aulas) {
        return aulas.stream().map(this::toDto).toList();
    }
}