package com.allgenda.aplicacao.allgenda.controller;

import com.allgenda.aplicacao.allgenda.dto.request.ImagemAulaUpdateDTO;
import com.allgenda.aplicacao.allgenda.dto.response.ImagemAulaResponseDTO;
import com.allgenda.aplicacao.allgenda.mapper.ImagemAulaMapper;
import com.allgenda.framework.service.ImagemArquivo;
import com.allgenda.framework.service.ImagemEncontroService;
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
import java.util.List;
import java.util.UUID;

// Imagens avulsas da aula, que não pertencem a nenhuma anotação.
@RestController
@RequestMapping("/api/aulas/{aulaId}/imagens")
public class ImagemAulaController {

    private final ImagemEncontroService service;
    private final ImagemAulaMapper mapper;

    public ImagemAulaController(ImagemEncontroService service, ImagemAulaMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    // o arquivo vai no campo "arquivo" do formulário e a legenda, opcional, no campo "legenda"
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImagemAulaResponseDTO> adicionar(@PathVariable UUID aulaId,
                                                           @RequestParam("arquivo") MultipartFile arquivo,
                                                           @RequestParam(value = "legenda", required = false) String legenda) {
        ImagemAulaResponseDTO criada = mapper.toDto(service.adicionar(aulaId, arquivo, legenda));
        return ResponseEntity.created(URI.create(criada.url())).body(criada);
    }

    @GetMapping
    public List<ImagemAulaResponseDTO> listar(@PathVariable UUID aulaId) {
        return mapper.toDtoList(service.listarPorEncontro(aulaId));
    }

    // corpo: todos os ids das imagens da aula, na nova ordem, ex: ["id1", "id2"]
    @PutMapping("/ordem")
    public List<ImagemAulaResponseDTO> reordenar(@PathVariable UUID aulaId, @RequestBody List<UUID> ids) {
        return mapper.toDtoList(service.reordenar(aulaId, ids));
    }

    @GetMapping("/{imagemId}")
    public ResponseEntity<Resource> baixar(@PathVariable UUID aulaId, @PathVariable UUID imagemId) {
        ImagemArquivo imagem = service.carregar(aulaId, imagemId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagem.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(imagem.nome(), StandardCharsets.UTF_8).build().toString())
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(imagem.conteudo());
    }

    @PatchMapping("/{imagemId}")
    public ImagemAulaResponseDTO editarLegenda(@PathVariable UUID aulaId, @PathVariable UUID imagemId,
                                               @RequestBody ImagemAulaUpdateDTO dto) {
        return mapper.toDto(service.editarLegenda(aulaId, imagemId, dto.legenda()));
    }

    @DeleteMapping("/{imagemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable UUID aulaId, @PathVariable UUID imagemId) {
        service.remover(aulaId, imagemId);
    }
}
