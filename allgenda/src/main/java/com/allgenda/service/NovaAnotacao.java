package com.allgenda.service;

import com.allgenda.model.VisibilidadeAnotacao;

import java.util.List;
import java.util.UUID;

// Dados para criar uma anotação num encontro. Título ou conteúdo (ao menos um); tags e visibilidade são
// opcionais (visibilidade ausente = GRUPO). O autor é sempre a pessoa logada.
public record NovaAnotacao(UUID encontroId, String titulo, String conteudo, List<String> tags,
                           VisibilidadeAnotacao visibilidade) {}
