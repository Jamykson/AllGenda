package com.allgenda.service;

import com.allgenda.dto.response.TagResponseDTO;
import com.allgenda.model.Tag;

import java.util.List;
import java.util.Set;

public interface TagService {
    Set<Tag> buscarOuCriar(List<String> nomes);
    List<TagResponseDTO> listarTodas();
}
