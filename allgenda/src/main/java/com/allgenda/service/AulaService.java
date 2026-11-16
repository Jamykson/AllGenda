package com.allgenda.service;

import com.allgenda.model.Encontro;

import java.util.List;
import java.util.UUID;

public interface AulaService {
    Encontro cadastrar(DadosDoEncontro dados);
    List<Encontro> listarPorDisciplina(UUID disciplinaId);
    Encontro buscarPorId(UUID id);
    Encontro atualizar(UUID id, AtualizacaoDeEncontro atualizacao);
    void excluir(UUID id);
}
