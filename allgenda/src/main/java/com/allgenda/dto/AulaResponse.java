package com.allgenda.dto;

import java.time.LocalDate;
import java.util.UUID;

public record AulaResponse(UUID id, LocalDate data, String horario, String topico) {
}