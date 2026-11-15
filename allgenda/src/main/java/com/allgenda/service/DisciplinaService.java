package com.allgenda.service;

import com.allgenda.dto.request.DisciplinaRequestDTO;
import com.allgenda.dto.request.DisciplinaUpdateDTO;
import com.allgenda.dto.response.DisciplinaResponseDTO;

import java.util.List;
import java.util.UUID;

public interface DisciplinaService {
    DisciplinaResponseDTO cadastrar(DisciplinaRequestDTO dto);
    List<DisciplinaResponseDTO> listarDoUsuario();
    DisciplinaResponseDTO buscarPorId(UUID id);
    // só o dono edita; por ora, só a descrição
    DisciplinaResponseDTO atualizar(UUID id, DisciplinaUpdateDTO dto);
    void excluir(UUID id);
}