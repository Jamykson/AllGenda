package com.allgenda.service;

import com.allgenda.model.Participacao;

// Um participante aceito do grupo e quantas anotações do grupo ele escreveu nele (as privadas não entram na
// conta, para a lista não revelar que existem).
public record Participante(Participacao participacao, long anotacoesDoGrupo) {}
