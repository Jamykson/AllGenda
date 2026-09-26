package com.allgenda.dto.request;

import java.time.LocalDate;
import java.util.UUID;

public record AulaRequest(
    UUID disciplinaId,
    LocalDate data, 
    String horario, 
    String topico
    ) {}