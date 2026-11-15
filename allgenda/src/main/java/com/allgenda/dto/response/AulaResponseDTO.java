package com.allgenda.dto.response;

import java.time.LocalDate;
import java.util.UUID;

// anotacoes: quantas anotações do grupo a aula tem (as privadas não entram); o front usa para avisar antes de excluir

public record AulaResponseDTO(
    UUID id, 
    LocalDate data, 
    String horario, 
    String topico,
    UUID disciplinaId,
    String disciplinaNome,
    boolean cancelada,
    long anotacoes
    ) {}