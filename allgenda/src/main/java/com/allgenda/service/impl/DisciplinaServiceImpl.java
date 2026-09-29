package com.allgenda.service.impl;

import com.allgenda.dto.request.DisciplinaRequestDTO;
import com.allgenda.dto.response.DisciplinaResponseDTO;
import com.allgenda.exception.DisciplinaNaoEncontradaException;
import com.allgenda.mapper.DisciplinaMapper;
import com.allgenda.model.Disciplina;
import com.allgenda.repository.DisciplinaRepository;
import com.allgenda.service.DisciplinaService;
import org.springframework.stereotype.Service;
import com.allgenda.exception.RequisicaoInvalidaException;
import org.springframework.transaction.annotation.Transactional;

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
    public DisciplinaResponseDTO cadastrar(DisciplinaRequestDTO dto) {
        if(dto.nome() == null || dto.nome().isBlank()){
            throw new RequisicaoInvalidaException("O nome da disciplina é obrigatório.");
        }

        Disciplina disciplina = new Disciplina();
        disciplina.setNome(dto.nome());

        Disciplina salva = repository.save(disciplina);
        return mapper.toDto(salva); // mapeia a disciplina salva para um DisciplinaResponseDTO, para assim retornar ao controller o objeto adequado.
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisciplinaResponseDTO> listarTodas() {
        return mapper.toDtoList(repository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public DisciplinaResponseDTO buscarPorId(UUID id) {
        Disciplina disciplina = repository.findById(id)
            .orElseThrow(() -> new DisciplinaNaoEncontradaException(id));
        return mapper.toDto(disciplina);
    }
}