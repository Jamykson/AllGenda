package com.allgenda.service.impl;

import com.allgenda.dto.DisciplinaRequestDTO;
import com.allgenda.dto.DisciplinaResponseDTO;
import com.allgenda.dto.HorarioRecorrenteRequestDTO;
import com.allgenda.exception.DisciplinaNaoEncontradaException;
import com.allgenda.mapper.DisciplinaMapper;
import com.allgenda.model.Disciplina;
import com.allgenda.repository.DisciplinaRepository;
import com.allgenda.service.DisciplinaService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class DisciplinaServiceImpl
    implements DisciplinaService {

    private final DisciplinaRepository repository;

    private final DisciplinaMapper mapper;

    public DisciplinaServiceImpl(
        DisciplinaRepository repository,
        DisciplinaMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public DisciplinaResponseDTO criar(
        DisciplinaRequestDTO request
    ) {

        if (
            request == null ||
            request.nome() == null ||
            request.nome().isBlank()
        ) {
            throw new IllegalArgumentException(
                "O nome da disciplina é obrigatório."
            );
        }

        Disciplina disciplina =
            new Disciplina();

        disciplina.setNome(
            request.nome().trim()
        );

        Disciplina salva =
            repository.save(
                disciplina
            );

        return mapper.toDto(
            salva
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisciplinaResponseDTO> listar() {

        List<Disciplina> disciplinas =
            repository.findAll();

        disciplinas.sort(
            Comparator.comparing(
                Disciplina::getNome,
                String.CASE_INSENSITIVE_ORDER
            )
        );

        return mapper.toDtoList(
            disciplinas
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DisciplinaResponseDTO buscarPorId(
        UUID id
    ) {

        Disciplina disciplina =
            buscarEntidade(id);

        return mapper.toDto(
            disciplina
        );
    }

    @Override
    @Transactional
    public DisciplinaResponseDTO salvarHorario(
        UUID id,
        HorarioRecorrenteRequestDTO request
    ) {

        validarHorario(
            request
        );

        Disciplina disciplina =
            buscarEntidade(id);

        List<Integer> dias =
            new ArrayList<>(
                request.dias()
            );

        dias.sort(
            Integer::compareTo
        );

        disciplina.setDias(
            dias
        );

        disciplina.setHoraInicio(
            request.horaInicio()
        );

        disciplina.setHoraFim(
            request.horaFim()
        );

        disciplina.setDataInicio(
            request.dataInicio()
        );

        disciplina.setDataFim(
            request.dataFim()
        );

        Disciplina salva =
            repository.save(
                disciplina
            );

        return mapper.toDto(
            salva
        );
    }

    @Override
    @Transactional
    public DisciplinaResponseDTO removerHorario(
        UUID id
    ) {

        Disciplina disciplina =
            buscarEntidade(id);

        disciplina.setDias(
            new ArrayList<>()
        );

        disciplina.setHoraInicio(
            null
        );

        disciplina.setHoraFim(
            null
        );

        disciplina.setDataInicio(
            null
        );

        disciplina.setDataFim(
            null
        );

        Disciplina salva =
            repository.save(
                disciplina
            );

        return mapper.toDto(
            salva
        );
    }

    @Override
    @Transactional
    public void excluir(
        UUID id
    ) {

        Disciplina disciplina =
            buscarEntidade(id);

        repository.delete(
            disciplina
        );
    }

    private Disciplina buscarEntidade(
        UUID id
    ) {

        return repository
            .findById(id)
            .orElseThrow(
                () ->
                    new DisciplinaNaoEncontradaException(
                        "Disciplina não encontrada."
                    )
            );
    }

    private void validarHorario(
        HorarioRecorrenteRequestDTO request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                "O horário é obrigatório."
            );
        }

        if (
            request.dias() == null ||
            request.dias().isEmpty()
        ) {
            throw new IllegalArgumentException(
                "Selecione pelo menos um dia da semana."
            );
        }

        boolean diaInvalido =
            request.dias()
                .stream()
                .anyMatch(
                    dia ->
                        dia == null ||
                        dia < 1 ||
                        dia > 7
                );

        if (diaInvalido) {
            throw new IllegalArgumentException(
                "Os dias da semana devem estar entre 1 e 7."
            );
        }

        long quantidadeDias =
            request.dias()
                .stream()
                .distinct()
                .count();

        if (
            quantidadeDias !=
            request.dias().size()
        ) {
            throw new IllegalArgumentException(
                "Não é permitido repetir dias da semana."
            );
        }

        if (
            request.horaInicio() == null ||
            request.horaFim() == null
        ) {
            throw new IllegalArgumentException(
                "Informe o horário inicial e final."
            );
        }

        if (
            !request.horaFim()
                .isAfter(
                    request.horaInicio()
                )
        ) {
            throw new IllegalArgumentException(
                "O horário final deve ser depois do horário inicial."
            );
        }

        if (
            request.dataInicio() == null ||
            request.dataFim() == null
        ) {
            throw new IllegalArgumentException(
                "Informe a data inicial e final."
            );
        }

        if (
            request.dataFim()
                .isBefore(
                    request.dataInicio()
                )
        ) {
            throw new IllegalArgumentException(
                "A data final não pode ser anterior à data inicial."
            );
        }
    }
}