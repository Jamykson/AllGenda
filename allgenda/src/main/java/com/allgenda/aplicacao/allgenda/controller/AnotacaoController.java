package com.allgenda.aplicacao.allgenda.controller;
import com.allgenda.aplicacao.allgenda.dto.request.AnotacaoUpdateDTO;

import com.allgenda.aplicacao.allgenda.dto.request.AnotacaoRequestDTO;
import com.allgenda.aplicacao.allgenda.dto.request.AnotacaoVisibilidadeDTO;
import com.allgenda.aplicacao.allgenda.dto.response.AnotacaoResponseDTO;
import com.allgenda.framework.exception.RequisicaoInvalidaException;
import com.allgenda.aplicacao.allgenda.mapper.AnotacaoMapper;
import com.allgenda.framework.service.AnotacaoService;
import com.allgenda.framework.service.NovaAnotacao;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/anotacoes")
public class AnotacaoController {

    private final AnotacaoService service;
    private final AnotacaoMapper mapper;

    public AnotacaoController(AnotacaoService service, AnotacaoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<AnotacaoResponseDTO> cadastrar(@RequestBody AnotacaoRequestDTO dto) {
        AnotacaoResponseDTO criada = mapper.toDto(service.cadastrar(
                new NovaAnotacao(dto.aulaId(), dto.titulo(), dto.conteudo(), dto.tags(), dto.visibilidade())));
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    // filtros aceitos: ?aulaId=..., ?disciplinaId=... (opcionalmente com &tag=...) ou ?tag=...
    @GetMapping
    public List<AnotacaoResponseDTO> listar(@RequestParam(required = false) UUID aulaId,
                                            @RequestParam(required = false) UUID disciplinaId,
                                            @RequestParam(required = false) String tag) {
        if (aulaId != null) {
            if (disciplinaId != null || tag != null) {
                throw new RequisicaoInvalidaException("O filtro por aula não pode ser combinado com outros filtros.");
            }
            return mapper.toDtoList(service.listarPorEncontro(aulaId));
        }
        if (disciplinaId != null) {
            return mapper.toDtoList(service.listarPorGrupo(disciplinaId, tag));
        }
        if (tag != null) {
            return mapper.toDtoList(service.listarPorTag(tag));
        }
        throw new RequisicaoInvalidaException("Informe um filtro: aulaId, disciplinaId ou tag.");
    }

    @PostMapping("/{id}/tags") // corpo: lista de nomes de tags, ex: ["java", "poo"]
    public AnotacaoResponseDTO associarTags(@PathVariable UUID id, @RequestBody List<String> tags) {
        return mapper.toDto(service.associarTags(id, tags));
    }

    // remove só o vínculo com a anotação; a tag continua existindo para as demais.
    // O nome vai na query, e não no caminho, porque uma tag pode ter "/" (ex.: "C/C++").
    @DeleteMapping("/{id}/tags")
    public AnotacaoResponseDTO removerTag(@PathVariable UUID id, @RequestParam String nome) {
        return mapper.toDto(service.removerTag(id, nome));
    }

    @PatchMapping("/{id}")
    public AnotacaoResponseDTO editar(@PathVariable UUID id, @RequestBody AnotacaoUpdateDTO dto) {
        return mapper.toDto(service.editar(id, dto.titulo(), dto.conteudo()));
    }

    @PatchMapping("/{id}/visibilidade")
    public AnotacaoResponseDTO alterarVisibilidade(@PathVariable UUID id, @RequestBody AnotacaoVisibilidadeDTO dto) {
        return mapper.toDto(service.alterarVisibilidade(id, dto == null ? null : dto.visibilidade()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
