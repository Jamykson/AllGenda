package com.allgenda.service;

import java.time.LocalDate;
import java.util.UUID;

// Dados comuns para criar um encontro de um grupo; todos são obrigatórios.
public record DadosDoEncontro(UUID grupoId, LocalDate data, String horario, String topico) {}
