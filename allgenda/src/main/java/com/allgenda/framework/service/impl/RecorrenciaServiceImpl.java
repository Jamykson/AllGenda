package com.allgenda.framework.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.allgenda.framework.exception.GrupoNaoEncontradoException;
import com.allgenda.framework.exception.RecorrenciaNaoEncontradaException;
import com.allgenda.framework.exception.RequisicaoInvalidaException;
import com.allgenda.framework.model.Grupo;
import com.allgenda.framework.model.FrequenciaRecorrencia;
import com.allgenda.framework.model.Recorrencia;
import com.allgenda.framework.repository.GrupoRepository;
import com.allgenda.framework.repository.RecorrenciaRepository;
import com.allgenda.framework.acesso.AcessoAoGrupo;
import com.allgenda.framework.acesso.Acao;
import com.allgenda.framework.service.DadosDaRecorrencia;
import com.allgenda.framework.service.RecorrenciaService;
import com.allgenda.framework.service.UsuarioAutenticado;

@Service
public class RecorrenciaServiceImpl implements RecorrenciaService {

    private static final Pattern HORA = Pattern.compile("([01]\\d|2[0-3]):[0-5]\\d");

    private final RecorrenciaRepository repository;
    private final GrupoRepository grupoRepository;
    private final AcessoAoGrupo acesso;
    private final UsuarioAutenticado usuarioAutenticado;

    public RecorrenciaServiceImpl(RecorrenciaRepository repository, GrupoRepository grupoRepository,
                                  AcessoAoGrupo acesso,
                                  UsuarioAutenticado usuarioAutenticado) {
        this.repository = repository;
        this.grupoRepository = grupoRepository;
        this.acesso = acesso;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    @Override
    @Transactional(readOnly = true)
    public Recorrencia buscar(UUID disciplinaId) {
        exigirDisciplina(disciplinaId);                                        // 404
        acesso.exigir(Acao.VER_CONTEUDO, disciplinaId, "Somente participantes do grupo têm acesso a ele."); // 403
        return repository.findByGrupoId(disciplinaId)
                .orElseThrow(() -> new RecorrenciaNaoEncontradaException(disciplinaId));   // 404
    }

    @Override
    @Transactional(readOnly = true)
    public List<Recorrencia> listarDoUsuario() {
        return repository.findDosGruposDoUsuario(usuarioAutenticado.id());
    }

    // cria ou substitui: a disciplina tem no máximo uma recorrência
    @Override
    @Transactional
    public Recorrencia salvar(UUID disciplinaId, DadosDaRecorrencia dto) {
        Grupo disciplina = exigirDisciplina(disciplinaId);                // 404
        acesso.exigir(Acao.DEFINIR_RECORRENCIA, disciplinaId, "Você não tem permissão para definir a recorrência do grupo.");   // 403
        validar(dto);                                                          // 400

        Recorrencia recorrencia = repository.findByGrupoId(disciplinaId).orElseGet(() -> {
            Recorrencia nova = new Recorrencia();
            nova.setGrupo(disciplina);
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
        acesso.exigir(Acao.DEFINIR_RECORRENCIA, disciplinaId, "Você não tem permissão para remover a recorrência do grupo.");   // 403
        Recorrencia recorrencia = repository.findByGrupoId(disciplinaId)
                .orElseThrow(() -> new RecorrenciaNaoEncontradaException(disciplinaId));    // 404
        // as aulas já cadastradas (com anotações) continuam; só a regra que gera as próximas some
        repository.delete(recorrencia);
    }

    private Grupo exigirDisciplina(UUID disciplinaId) {
        return grupoRepository.findById(disciplinaId)
                .orElseThrow(() -> new GrupoNaoEncontradoException(disciplinaId));
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
