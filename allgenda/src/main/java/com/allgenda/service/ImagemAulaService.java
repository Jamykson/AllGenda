package com.allgenda.service;

import com.allgenda.model.ImagemAula;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

// Imagens avulsas de uma aula, sem anotação. Ver, baixar, enviar e reordenar exigem participação aceita na
// disciplina; editar a legenda e remover exigem ser o autor da imagem.
public interface ImagemAulaService {
    ImagemAula adicionar(UUID aulaId, MultipartFile arquivo, String legenda); // legenda opcional
    List<ImagemAula> listarPorAula(UUID aulaId);
    // ids: todas as imagens da aula, na nova ordem
    List<ImagemAula> reordenar(UUID aulaId, List<UUID> ids);
    ImagemArquivo carregar(UUID aulaId, UUID imagemId);
    // substitui a legenda; ausente ou vazia, a imagem fica sem legenda
    ImagemAula editarLegenda(UUID aulaId, UUID imagemId, String legenda);
    void remover(UUID aulaId, UUID imagemId);
}
