package com.allgenda.service;

import java.time.LocalDate;
import java.util.UUID;

// Dados comuns para criar um evento de um grupo. "subtipo" é o tipo de evento da aplicação
// (no AllGenda, PROVA ou ENTREGA), validado pela fábrica do tipo.
public record DadosDoEvento(UUID grupoId, String descricao, LocalDate data, String subtipo) {}
