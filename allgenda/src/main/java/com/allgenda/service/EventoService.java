package com.allgenda.service;

import com.allgenda.framework.extensao.DadosDoEvento;
import com.allgenda.model.Evento;

import java.util.List;
import java.util.UUID;

public interface EventoService {
    // cria um evento do tipo registrado pela aplicação; o subtipo é validado pela fábrica do tipo
    Evento criar(String tipo, DadosDoEvento dados);
    // em ordem de data
    List<Evento> listarPorGrupo(UUID grupoId);
}
