package com.allgenda.mapper;

import com.allgenda.dto.response.ImagemAulaResponseDTO;
import com.allgenda.model.ImagemEncontro;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ImagemAulaMapper {

    public ImagemAulaResponseDTO toDto(ImagemEncontro imagem) {
        return new ImagemAulaResponseDTO(
            imagem.getId(),
            imagem.getLegenda(),
            imagem.getNomeOriginal(),
            imagem.getContentType(),
            imagem.getTamanho(),
            "/api/aulas/" + imagem.getEncontro().getId() + "/imagens/" + imagem.getId(),
            imagem.getDataCriacao(),
            imagem.getAutor().getId(),
            imagem.getAutor().getNome(),
            imagem.getEncontro().getId(),
            imagem.getPosicao()
        );
    }

    public List<ImagemAulaResponseDTO> toDtoList(List<ImagemEncontro> imagens) {
        return imagens.stream().map(this::toDto).toList();
    }
}
