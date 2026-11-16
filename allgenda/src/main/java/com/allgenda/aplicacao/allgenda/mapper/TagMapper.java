package com.allgenda.aplicacao.allgenda.mapper;

import com.allgenda.aplicacao.allgenda.dto.response.TagResponseDTO;
import com.allgenda.framework.model.Tag;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TagMapper {

    public TagResponseDTO toDto(Tag tag) {
        return new TagResponseDTO(
            tag.getId(),
            tag.getNome(),
            tag.getGrupo().getId()
        );
    }

    public List<TagResponseDTO> toDtoList(List<Tag> tags) {
        return tags.stream().map(this::toDto).toList();
    }
}
