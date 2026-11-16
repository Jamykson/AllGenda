package com.allgenda.service;

// Dados comuns para criar um grupo: o núcleo valida o nome (obrigatório) e a descrição (até 500 caracteres).
public record DadosDoGrupo(String nome, String descricao) {}
