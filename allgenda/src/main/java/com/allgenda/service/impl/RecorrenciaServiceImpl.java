package com.allgenda.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.allgenda.exception.DisciplinaNaoEncontradaException;
import com.allgenda.exception.RecorrenciaNaoEncontradaException;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.model.Disciplina;
import com.allgenda.model.FrequenciaRecorrencia;
import com.allgenda.model.Recorrencia;
import com.allgenda.repository.DisciplinaRepository;
import com.allgenda.repository.RecorrenciaRepository;
import com.allgenda.service.AcessoDisciplina;
import com.allgenda.service.DadosDaRecorrencia;
import com.allgenda.service.RecorrenciaService;
import com.allgenda.service.UsuarioAutenticado;

@Service
public class RecorrenciaServiceImpl implements RecorrenciaService {

    private static final Pattern HORA = Pattern.compile("([01]\\d|2[0-3]):[0-5]\\d");

    private final RecorrenciaRepository repository;
    private final DisciplinaRepository disciplinaRepository;
    private final AcessoDisciplina acesso;
    private final UsuarioAutenticado usuarioAutenticado;

    public RecorrenciaServiceImpl(RecorrenciaRepository repository, DisciplinaRepository disciplinaRepository,
                                  AcessoDisciplina acesso,
                                  UsuarioAutenticado usuarioAutenticado) {
        this.repository = repository;
        this.disciplinaRepository = disciplinaRepository;
        this.acesso = acesso;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    @Override
    @Transactional(readOnly = true)
    public Recorrencia buscar(UUID disciplinaId) {
        exigirDisciplina(disciplinaId);                                        // 404
        acesso.exigirParticipanteAceito(disciplinaId);                         // 403
        return repository.findByDisciplinaId(disciplinaId)
                .orElseThrow(() -> new RecorrenciaNaoEncontradaException(disciplinaId));   // 404
    }

    @Override
    @Transactional(readOnly = true)
    public List<Recorrencia> listarDoUsuario() {
        return repository.findDasDisciplinasDoUsuario(usuarioAutenticado.id());
    }

    // cria ou substitui: a disciplina tem no máximo uma recorrência
    @Override
    @Transactional
    public Recorrencia salvar(UUID disciplinaId, DadosDaRecorrencia dto) {
        Disciplina disciplina = exigirDisciplina(disciplinaId);                // 404
        acesso.exigirDono(disciplinaId, "Somente o dono pode definir a recorrência da disciplina.");   // 403
        validar(dto);                                                          // 400

        Recorrencia recorrencia = repository.findByDisciplinaId(disciplinaId).orElseGet(() -> {
            Recorrencia nova = new Recorrencia();
            nova.setDisciplina(disciplina);
            return nova;
        });
        recorrencia.setDiasSemana(dto.diasSemana());
        recorrencia.setHoraInicio(dto.horaInicio());
        recorrencia.setHoraFim(dto.horaFim());
        recorrencia.setDataInicio(dto.dataInicio());
        recorrencia.setDataFim(dto.dataFim());
        recorrencia.setFrequencia(dto.frequencia() == null ? FrequenciaRecorrencia.SEMANAL : dto.frequencia());
        return repository.save(recorrencia);
    }

    @Override
    @Transactional
    public void remover(UUID disciplinaId) {
        exigirDisciplina(disciplinaId);                                        // 404
        acesso.exigirDono(disciplinaId, "Somente o dono pode remover a recorrência da disciplina.");   // 403
        Recorrencia recorrencia = repository.findByDisciplinaId(disciplinaId)
                .orElseThrow(() -> new RecorrenciaNaoEncontradaException(disciplinaId));    // 404
        // as aulas já cadastradas (com anotações) continuam; só a regra que gera as próximas some
        repository.delete(recorrencia);
    }

    private Disciplina exigirDisciplina(UUID disciplinaId) {
        return disciplinaRepository.findById(disciplinaId)
                .orElseThrow(() -> new DisciplinaNaoEncontradaException(disciplinaId));
    }

    private void validar(DadosDaRecorrencia dto) {
        if (dto == null || dto.diasSemana() == null || dto.diasSemana().isEmpty()) {
            throw new RequisicaoInvalidaException("Informe pelo menos um dia da semana.");
        }
        if (dto.horaInicio() == null || !HORA.matcher(dto.horaInicio()).matches()
                || dto.horaFim() == null || !HORA.matcher(dto.horaFim()).matches()) {
            throw new RequisicaoInvalidaException("Informe o horário de início e de fim no formato HH:mm.");
        }
        if (dto.horaFim().compareTo(dto.horaInicio()) <= 0) {
            throw new RequisicaoInvalidaException("O horário final deve ser depois do horário inicial.");
        }
        if (dto.dataInicio() == null || dto.dataFim() == null) {
            throw new RequisicaoInvalidaException("Informe a data inicial e a final.");
        }
        if (dto.dataFim().isBefore(dto.dataInicio())) {
            throw new RequisicaoInvalidaException("A data final deve ser depois da data inicial.");
        }
    }
}
