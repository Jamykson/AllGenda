package com.allgenda.service;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.allgenda.exception.DisciplinaNaoEncontradaException;
import com.allgenda.model.Aula;
import com.allgenda.model.Disciplina;
import com.allgenda.repository.AulaRepository;
import com.allgenda.repository.DisciplinaRepository;

@Service
public class AulaService {

    @Autowired
    private AulaRepository aulaRepository;

    @Autowired
    private DisciplinaRepository disciplinaRepository;

    public Aula criar(LocalDate data, String horario, String topico, UUID disciplinaId) {
        Disciplina disciplina = disciplinaRepository.findById(disciplinaId)
                .orElseThrow(() -> new DisciplinaNaoEncontradaException(disciplinaId));

        Aula aula = new Aula();
        aula.setData(data);
        aula.setHorario(horario);
        aula.setTopico(topico);
        aula.setDisciplina(disciplina);

        return aulaRepository.save(aula);
    }
}