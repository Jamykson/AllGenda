package com.allgenda.framework.service;

import java.util.List;
import java.util.UUID;

import com.allgenda.framework.model.Participacao;
import com.allgenda.framework.model.StatusParticipacao;

// Um convite é uma participação de membro ainda pendente.
public interface ConviteService {
    Participacao enviar(UUID disciplinaId, String email);
    List<Participacao> listar(StatusParticipacao status); // convites do usuário logado
    Participacao aceitar(UUID conviteId);
    Participacao recusar(UUID conviteId);
}
