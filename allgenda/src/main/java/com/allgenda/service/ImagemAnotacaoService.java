package com.allgenda.service;

import com.allgenda.model.ImagemAnotacao;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ImagemAnotacaoService {
    ImagemAnotacao anexar(UUID anotacaoId, MultipartFile arquivo);
    ImagemArquivo carregar(UUID anotacaoId, UUID imagemId);
    void remover(UUID anotacaoId, UUID imagemId);
}
