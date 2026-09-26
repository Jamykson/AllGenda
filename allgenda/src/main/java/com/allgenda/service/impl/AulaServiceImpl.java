package com.allgenda.service.impl;

import com.allgenda.dto.request.AulaRequestDTO;
import com.allgenda.dto.response.AulaResponseDTO;
import com.allgenda.exception.AulaNaoEncontradaException;
import com.allgenda.exception.DisciplinaNaoEncontradaException;
import com.allgenda.mapper.AulaMapper;
import com.allgenda.model.Aula;
import com.allgenda.model.Disciplina;
import com.allgenda.repository.AulaRepository;
import com.allgenda.repository.DisciplinaRepository;
import com.allgenda.service.AulaService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AulaServiceImpl implements AulaService {

    private final AulaRepository aulaRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final AulaMapper mapper;

    public AulaServiceImpl(AulaRepository aulaRepository,
                            DisciplinaRepository disciplinaRepository,
                            AulaMapper mapper) {
        this.aulaRepository = aulaRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.mapper = mapper;
    }

    @Override
    public AulaResponseDTO cadastrar(AulaRequestDTO dto) {
        Disciplina disciplina = disciplinaRepository.findById(dto.disciplinaId())
                .orElseThrow(() -> new DisciplinaNaoEncontradaException(dto.disciplinaId()));

        Aula aula = new Aula();
        aula.setData(dto.data());
        aula.setHorario(dto.horario());
        aula.setTopico(dto.topico());
        aula.setDisciplina(disciplina);

        Aula salva = aulaRepository.save(aula);
        return mapper.toDto(salva);
    }

    @Override
    public List<AulaResponseDTO> listarPorDisciplina(UUID disciplinaId) {
        return mapper.toDtoList(aulaRepository.findByDisciplinaId(disciplinaId));
    }

    @Override
    public AulaResponseDTO buscarPorId(UUID id) {
        Aula aula = aulaRepository.findById(id)
                .orElseThrow(() -> new AulaNaoEncontradaException(id));
        return mapper.toDto(aula);
    }
}