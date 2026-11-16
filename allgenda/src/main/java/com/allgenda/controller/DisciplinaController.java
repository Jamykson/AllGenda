package com.allgenda.controller;

import com.allgenda.dto.request.DisciplinaRequestDTO;
import com.allgenda.dto.request.DisciplinaUpdateDTO;
import com.allgenda.dto.response.DisciplinaResponseDTO;
import com.allgenda.mapper.DisciplinaMapper;
import com.allgenda.framework.extensao.DadosDoGrupo;
import com.allgenda.model.Disciplina;
import com.allgenda.service.GrupoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/disciplinas") // prefixo em comum de todos os endpoints da classe
public class DisciplinaController {

    private final GrupoService service;
    private final DisciplinaMapper mapper;

    public DisciplinaController(GrupoService service, DisciplinaMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping // responde a requisições POST (criar algo novo no servidor)
    public ResponseEntity<DisciplinaResponseDTO> cadastrar (@RequestBody DisciplinaRequestDTO dto) { // @RequestBody diz ao Spring "pegue o JSON do corpo da requisição e converta automaticamente pro tipo especificado" (usando os nomes dos campos do record pra casar com as chaves do JSON).
        DisciplinaResponseDTO criada = mapper.toDto(service.criar(Disciplina.TIPO, new DadosDoGrupo(dto.nome(), dto.descricao())));
        return ResponseEntity.status(HttpStatus.CREATED).body(criada); // Para um POST que cria algo, a convenção REST correta é devolver 201 Created, por isso definir o status manualmente.
    }

    @GetMapping // responde a requisições GET (buscar dado, sem alterar nada no servidor)
    public List<DisciplinaResponseDTO> listarDoUsuario() {
        return mapper.toDtoList(service.listarDoUsuario());
    }

    @GetMapping("/{id}") // sufixo específico desse endpoint
    public DisciplinaResponseDTO buscarPorId(@PathVariable UUID id) { // @PathVariable extrai o id da URL e já converte pro tipo UUID.
        return mapper.toDto(service.buscarPorId(id)); // busca por ID assume que o cliente já obteve o id de algum jeito antes.
    }

    @PatchMapping("/{id}")
    public DisciplinaResponseDTO atualizar(@PathVariable UUID id, @RequestBody DisciplinaUpdateDTO dto) {
        return mapper.toDto(service.atualizarDescricao(id, dto == null ? null : dto.descricao()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}