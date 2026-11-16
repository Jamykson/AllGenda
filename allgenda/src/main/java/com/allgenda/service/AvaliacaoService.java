package com.allgenda.service;

import com.allgenda.model.Evento;

import java.util.List;
import java.util.UUID;

public interface AvaliacaoService {
    Evento cadastrar(DadosDoEvento dados);
    List<Evento> listarPorDisciplina(UUID disciplinaId);
}
