package com.allgenda.service;

import java.util.List;
import java.util.UUID;

// Quem participa de uma disciplina: qualquer participante aceito vê a lista; só o dono remove um membro.
public interface ParticipanteService {
    // participações ACEITO, o dono primeiro e depois os membros em ordem alfabética
    List<Participante> listar(UUID disciplinaId);

    // apaga a participação do membro, que pode ser convidado de novo; as anotações do grupo dele continuam
    // na disciplina e as privadas ficam guardadas, sem que ninguém as veja. O dono não pode ser removido.
    void remover(UUID disciplinaId, UUID usuarioId);
}
