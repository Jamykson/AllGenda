package com.allgenda.service;

import java.util.List;
import java.util.UUID;

import com.allgenda.dto.request.ConviteRequestDTO;
import com.allgenda.dto.response.ConviteResponseDTO;
import com.allgenda.model.StatusParticipacao;

public interface ConviteService {
    ConviteResponseDTO enviar(UUID disciplinaId, ConviteRequestDTO dto);
    List<ConviteResponseDTO> listar(StatusParticipacao status); // convites do usuário logado
    ConviteResponseDTO aceitar(UUID conviteId);
    ConviteResponseDTO recusar(UUID conviteId);
}
