package com.allgenda.service;

import com.allgenda.dto.request.DisciplinaRequestDTO;
import com.allgenda.dto.response.DisciplinaResponseDTO;

import java.util.List;
import java.util.UUID;

public interface DisciplinaService {
    DisciplinaResponseDTO cadastrar(DisciplinaRequestDTO dto);
    List<DisciplinaResponseDTO> listarTodas();
    DisciplinaResponseDTO buscarPorId(UUID id);
}