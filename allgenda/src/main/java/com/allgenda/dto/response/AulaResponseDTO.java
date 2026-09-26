package com.allgenda.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record AulaResponse(
    UUID id, 
    LocalDate data, 
    String horario, 
    String topico,
    String disciplinaNome
    ) {}