package com.allgenda.service;

import com.allgenda.dto.request.AulaRequestDTO;
import com.allgenda.dto.response.AulaResponseDTO;

import java.util.List;
import java.util.UUID;

public interface AulaService {
    AulaResponseDTO cadastrar(AulaRequestDTO dto);
    List<AulaResponseDTO> listarPorDisciplina(UUID disciplinaId);
    AulaResponseDTO buscarPorId(UUID id);
}