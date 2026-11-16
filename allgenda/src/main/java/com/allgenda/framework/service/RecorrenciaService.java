package com.allgenda.framework.service;

import java.util.List;
import java.util.UUID;

import com.allgenda.framework.model.Recorrencia;

public interface RecorrenciaService {
    Recorrencia buscar(UUID grupoId);
    List<Recorrencia> listarDoUsuario();
    Recorrencia salvar(UUID grupoId, DadosDaRecorrencia dados);
    void remover(UUID grupoId);
}
