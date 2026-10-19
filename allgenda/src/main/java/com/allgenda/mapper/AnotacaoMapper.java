package com.allgenda.mapper;

import com.allgenda.dto.response.AnotacaoResponseDTO;
import com.allgenda.dto.response.ImagemAnotacaoResponseDTO;
import com.allgenda.model.Anotacao;
import com.allgenda.model.ImagemAnotacao;
import com.allgenda.model.Tag;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class AnotacaoMapper {

    public AnotacaoResponseDTO toDto(Anotacao anotacao) {
        List<String> nomesDasTags = anotacao.getTags().stream()
                .map(Tag::getNome)
                .toList();

        return new AnotacaoResponseDTO(
            anotacao.getId(),
            anotacao.getConteudo(),
            anotacao.getDataCriacao(),
            anotacao.getAutor().getId(),
            anotacao.getAutor().getNome(),
            nomesDasTags,
            anotacao.getAula().getDisciplina().getNome(),
            anotacao.getAula().getId(),
            anotacao.getAula().getData(),
            anotacao.getAula().getTopico(),
            anotacao.getImagens().stream().map(this::toImagemDto).toList()
        );
    }

    public List<AnotacaoResponseDTO> toDtoList(List<Anotacao> anotacoes) {
        return anotacoes.stream().map(this::toDto).toList();
    }

    public ImagemAnotacaoResponseDTO toImagemDto(ImagemAnotacao imagem) {
        return new ImagemAnotacaoResponseDTO(
            imagem.getId(),
            imagem.getNomeOriginal(),
            imagem.getContentType(),
            imagem.getTamanho(),
            "/api/anotacoes/" + imagem.getAnotacao().getId() + "/imagens/" + imagem.getId()
        );
    }
}
