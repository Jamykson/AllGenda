package com.allgenda.dto;

import java.time.LocalDate;

public record AulaRequest(LocalDate data, String horario, String topico) {
}