package com.allgenda.service;

import java.util.List;
import java.util.UUID;

import com.allgenda.dto.request.RecorrenciaRequestDTO;
import com.allgenda.dto.response.RecorrenciaResponseDTO;

public interface RecorrenciaService {
    RecorrenciaResponseDTO buscar(UUID disciplinaId);
    List<RecorrenciaResponseDTO> listarDoUsuario();
    RecorrenciaResponseDTO salvar(UUID disciplinaId, RecorrenciaRequestDTO dto);
    void remover(UUID disciplinaId);
}
