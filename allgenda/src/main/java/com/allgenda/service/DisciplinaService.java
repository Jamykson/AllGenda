package com.allgenda.service;

import com.allgenda.dto.DisciplinaRequestDTO;
import com.allgenda.dto.DisciplinaResponseDTO;
import com.allgenda.dto.HorarioRecorrenteRequestDTO;

import java.util.List;
import java.util.UUID;

public interface DisciplinaService {

    DisciplinaResponseDTO criar(
        DisciplinaRequestDTO request
    );

    List<DisciplinaResponseDTO> listar();

    DisciplinaResponseDTO buscarPorId(
        UUID id
    );

    DisciplinaResponseDTO salvarHorario(
        UUID id,
        HorarioRecorrenteRequestDTO request
    );

    DisciplinaResponseDTO removerHorario(
        UUID id
    );

    void excluir(
        UUID id
    );
}