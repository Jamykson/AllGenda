package com.allgenda.aplicacao.allgenda.dto.request;

// substitui a descrição: ausente ou em branco, a disciplina fica sem descrição (máximo de 500 caracteres)
public record DisciplinaUpdateDTO(String descricao) {}
