package com.allgenda.framework.service;

import java.util.List;
import java.util.UUID;

// Quem participa de um grupo: a política diz quem vê a lista (VER_CONTEUDO) e quem remove (REMOVER_MEMBRO).
public interface ParticipanteService {
    // participações ACEITO, quem criou o grupo primeiro e depois os demais em ordem alfabética
    List<Participante> listar(UUID grupoId);

    // apaga a participação do membro, que pode ser convidado de novo; as anotações do grupo dele continuam
    // no grupo e as privadas ficam guardadas, sem que ninguém as veja. Quem criou o grupo não pode ser removido.
    void remover(UUID grupoId, UUID usuarioId);
}
