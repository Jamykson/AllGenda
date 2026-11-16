package com.allgenda.framework.service;

import java.util.List;
import java.util.UUID;

import com.allgenda.framework.model.Recorrencia;

public interface RecorrenciaService {
    Recorrencia buscar(UUID disciplinaId);
    List<Recorrencia> listarDoUsuario();
    Recorrencia salvar(UUID disciplinaId, DadosDaRecorrencia dados);
    void remover(UUID disciplinaId);
}
