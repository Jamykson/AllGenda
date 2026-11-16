package com.allgenda.framework.service;

import com.allgenda.framework.model.ImagemEncontro;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

// Imagens avulsas de um encontro (a galeria), sem anotação. Ver e baixar exigem VER_CONTEUDO; enviar e
// reordenar, CRIAR_ANOTACAO; editar a legenda e remover exigem ser o autor da imagem (regra fixa).
public interface ImagemEncontroService {
    ImagemEncontro adicionar(UUID encontroId, MultipartFile arquivo, String legenda); // legenda opcional
    List<ImagemEncontro> listarPorEncontro(UUID encontroId);
    // ids: todas as imagens do encontro, na nova ordem
    List<ImagemEncontro> reordenar(UUID encontroId, List<UUID> ids);
    ImagemArquivo carregar(UUID encontroId, UUID imagemId);
    // substitui a legenda; ausente ou vazia, a imagem fica sem legenda
    ImagemEncontro editarLegenda(UUID encontroId, UUID imagemId, String legenda);
    void remover(UUID encontroId, UUID imagemId);
}
