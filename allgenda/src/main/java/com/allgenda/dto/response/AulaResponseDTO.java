package com.allgenda.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record AulaResponseDTO(
    UUID id, 
    LocalDate data, 
    String horario, 
    String topico,
    String disciplinaNome
    ) {}