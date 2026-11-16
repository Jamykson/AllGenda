package com.allgenda.service;

import com.allgenda.model.Aula;

import java.util.List;
import java.util.UUID;

public interface AulaService {
    Aula cadastrar(DadosDoEncontro dados);
    List<Aula> listarPorDisciplina(UUID disciplinaId);
    Aula buscarPorId(UUID id);
    Aula atualizar(UUID id, AtualizacaoDeEncontro atualizacao);
    void excluir(UUID id);
}
