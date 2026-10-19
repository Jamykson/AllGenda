package com.allgenda.service;

import com.allgenda.dto.request.ImagemAulaUpdateDTO;
import com.allgenda.dto.response.ImagemAulaResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

// Imagens avulsas de uma aula, sem anotação. Ver, baixar, enviar e reordenar exigem participação aceita na
// disciplina; editar a legenda e remover exigem ser o autor da imagem.
public interface ImagemAulaService {
    ImagemAulaResponseDTO adicionar(UUID aulaId, MultipartFile arquivo, String legenda); // legenda opcional
    List<ImagemAulaResponseDTO> listarPorAula(UUID aulaId);
    // ids: todas as imagens da aula, na nova ordem
    List<ImagemAulaResponseDTO> reordenar(UUID aulaId, List<UUID> ids);
    ImagemArquivo carregar(UUID aulaId, UUID imagemId);
    ImagemAulaResponseDTO editarLegenda(UUID aulaId, UUID imagemId, ImagemAulaUpdateDTO dto);
    void remover(UUID aulaId, UUID imagemId);
}
