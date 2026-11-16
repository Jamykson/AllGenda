package com.allgenda.service.impl;

import com.allgenda.exception.GrupoNaoEncontradoException;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.model.Avaliacao;
import com.allgenda.model.Evento;
import com.allgenda.model.Grupo;
import com.allgenda.model.TipoAvaliacao;
import com.allgenda.repository.EventoRepository;
import com.allgenda.repository.GrupoRepository;
import com.allgenda.service.AvaliacaoService;
import com.allgenda.service.DadosDoEvento;
import com.allgenda.service.AcessoDisciplina;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class AvaliacaoServiceImpl implements AvaliacaoService {

    private static final String SEM_ACESSO = "Somente participantes da disciplina acessam as avaliações.";

    private final EventoRepository avaliacoes;
    private final GrupoRepository disciplinas;
    private final AcessoDisciplina acesso;

    public AvaliacaoServiceImpl(EventoRepository avaliacoes,
                               GrupoRepository disciplinas,
                               AcessoDisciplina acesso) {
        this.avaliacoes = avaliacoes;
        this.disciplinas = disciplinas;
        this.acesso = acesso;
    }

    @Override
    @Transactional
    public Evento cadastrar(DadosDoEvento dto) {
        TipoAvaliacao tipo = validar(dto);
        Grupo disciplina = disciplinas.findById(dto.grupoId())
                .orElseThrow(() -> new GrupoNaoEncontradoException(dto.grupoId()));  //404
        acesso.exigirParticipanteAceito(disciplina.getId(), SEM_ACESSO);
        // A referência garante que a avaliação pertença a uma disciplina existente.
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setGrupo(disciplina);
        avaliacao.setDescricao(dto.descricao().trim());
        avaliacao.setData(dto.data());
        avaliacao.setTipo(tipo);
        return avaliacoes.save(avaliacao);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Evento> listarPorDisciplina(UUID disciplinaId) {
        if (disciplinaId == null) {
            throw new RequisicaoInvalidaException("O id da disciplina é obrigatório.");
        }
        // Diferencia uma disciplina sem avaliações de uma disciplina inexistente.
        if (!disciplinas.existsById(disciplinaId)) {
            throw new GrupoNaoEncontradoException(disciplinaId);
        }
        acesso.exigirParticipanteAceito(disciplinaId, SEM_ACESSO);
        return avaliacoes.findByGrupoIdOrderByDataAscIdAsc(disciplinaId);
    }

    private TipoAvaliacao validar(DadosDoEvento dto) {
        if (dto.grupoId() == null) {
            throw new RequisicaoInvalidaException("O id da disciplina é obrigatório.");
        }
        if (dto.descricao() == null || dto.descricao().isBlank() || dto.descricao().trim().length() > 500) {
            throw new RequisicaoInvalidaException("Informe uma descrição de até 500 caracteres.");
        }
        if (dto.data() == null) {
            throw new RequisicaoInvalidaException("A data da avaliação é obrigatória.");
        }
        try {
            return TipoAvaliacao.valueOf(dto.subtipo());
        } catch (NullPointerException | IllegalArgumentException e) {
            throw new RequisicaoInvalidaException("O tipo da avaliação é obrigatório: PROVA ou ENTREGA.");
        }
    }

}
