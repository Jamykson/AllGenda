package com.allgenda.service;

import com.allgenda.dto.response.ImagemAnotacaoResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ImagemAnotacaoService {
    ImagemAnotacaoResponseDTO anexar(UUID anotacaoId, MultipartFile arquivo);
    ImagemArquivo carregar(UUID anotacaoId, UUID imagemId);
    void remover(UUID anotacaoId, UUID imagemId);
}
