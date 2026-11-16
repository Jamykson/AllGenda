package com.allgenda.service;

import com.allgenda.model.Avaliacao;

import java.util.List;
import java.util.UUID;

public interface AvaliacaoService {
    Avaliacao cadastrar(DadosDoEvento dados);
    List<Avaliacao> listarPorDisciplina(UUID disciplinaId);
}
