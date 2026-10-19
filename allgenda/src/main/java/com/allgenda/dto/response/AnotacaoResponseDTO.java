package com.allgenda.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

public record AnotacaoResponseDTO(
    UUID id,
    String conteudo,
    LocalDateTime dataCriacao,
    UUID autorId, // o front compara com o id do usuário logado para saber se a anotação é dele
    String autorNome,
    List<String> tags,
    String disciplinaNome,
    UUID aulaId,
    LocalDate aulaData,
    String aulaTopico,
    List<ImagemAnotacaoResponseDTO> imagens
) {}