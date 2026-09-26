package com.allgenda.dto.request.impl;

import com.allgenda.dto.request.DisciplinaRequestDTO;
import com.allgenda.dto.response.DisciplinaResponseDTO;
import com.allgenda.dto.exception.DisciplinaNaoEncontradaException;
import com.allgenda.mapper.DisciplinaMapper;
import com.allgenda.model.Disciplina;
import com.allgenda.repository.DisciplinaRepository;
import com.allgenda.service.DisciplinaService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DisciplinaServiceImpl implements DisciplinaService {

    private final DisciplinaRepository repository;
    private final DisciplinaMapper mapper;

    public DisciplinaServiceImpl(DisciplinaRepository repository, DisciplinaMapper mapper){
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public DisciplinaReponseDTO cadastrar(DisciplinaRequestDTO dto) {
        Disciplina disciplina = new Disciplina();
        disciplina.setNome(dto.nome());

        Disciplina salva = repository.save(disciplina);
        return mapper.toDto(salva); // mapeia a disciplina salva para um DisciplinaResponseDTO, para assim retornar ao controller o objeto adequado.
    }

    @Override
    public List<DisciplinaReponseDTO> listarTodas() {
        return mapper.toDtoList(repository.findAll());
    }

    @Override
    public DisciplinaReponseDTO buscarPorId(UUID id) {
        Disciplina disciplina = repository.findById(id)
            .orElseThrow(() -> new DisciplinaNaoEncontradaException("Disciplina nao encontrada: " + id));
        return mapper.toDto(disciplina);
    }
}