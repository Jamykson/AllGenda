package com.allgenda.controller;

import com.allgenda.dto.response.ImagemAnotacaoResponseDTO;
import com.allgenda.service.ImagemAnotacaoService;
import com.allgenda.service.ImagemArquivo;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/anotacoes/{anotacaoId}/imagens")
public class ImagemAnotacaoController {

    private final ImagemAnotacaoService service;

    public ImagemAnotacaoController(ImagemAnotacaoService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // o arquivo vai no campo "arquivo" do formulário
    public ResponseEntity<ImagemAnotacaoResponseDTO> anexar(@PathVariable UUID anotacaoId,
                                                            @RequestParam("arquivo") MultipartFile arquivo) {
        ImagemAnotacaoResponseDTO criada = service.anexar(anotacaoId, arquivo);
        return ResponseEntity.created(URI.create(criada.url())).body(criada);
    }

    @GetMapping("/{imagemId}")
    public ResponseEntity<Resource> baixar(@PathVariable UUID anotacaoId, @PathVariable UUID imagemId) {
        ImagemArquivo imagem = service.carregar(anotacaoId, imagemId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagem.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(imagem.nome(), StandardCharsets.UTF_8).build().toString())
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(imagem.conteudo());
    }

    @DeleteMapping("/{imagemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable UUID anotacaoId, @PathVariable UUID imagemId) {
        service.remover(anotacaoId, imagemId);
    }
}
