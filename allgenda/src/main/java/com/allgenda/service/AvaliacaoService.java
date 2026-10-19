package com.allgenda.service;

import com.allgenda.dto.request.AvaliacaoRequestDTO;
import com.allgenda.dto.response.AvaliacaoResponseDTO;
import java.util.List;
import java.util.UUID;

public interface AvaliacaoService {
    AvaliacaoResponseDTO cadastrar(AvaliacaoRequestDTO dto);
    List<AvaliacaoResponseDTO> listarPorDisciplina(UUID disciplinaId);
}
