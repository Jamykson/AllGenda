package com.allgenda.service;

import com.allgenda.dto.request.AnotacaoRequestDTO;
import com.allgenda.dto.response.AnotacaoResponseDTO;

import java.util.List;
import java.util.UUID;

public interface AnotacaoService {
    AnotacaoResponseDTO cadastrar(AnotacaoRequestDTO dto);
    AnotacaoResponseDTO associarTags(UUID anotacaoId, List<String> nomesTags);
}
