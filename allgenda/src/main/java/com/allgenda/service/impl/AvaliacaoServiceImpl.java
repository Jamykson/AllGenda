package com.allgenda.service.impl;

import com.allgenda.dto.request.AvaliacaoRequestDTO;
import com.allgenda.dto.response.AvaliacaoResponseDTO;
import com.allgenda.exception.DisciplinaNaoEncontradaException;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.mapper.AvaliacaoMapper;
import com.allgenda.model.Avaliacao;
import com.allgenda.model.Disciplina;
import com.allgenda.repository.AvaliacaoRepository;
import com.allgenda.repository.DisciplinaRepository;
import com.allgenda.service.AvaliacaoService;
import com.allgenda.service.AcessoDisciplina;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class AvaliacaoServiceImpl implements AvaliacaoService {

    private static final String SEM_ACESSO = "Somente participantes da disciplina acessam as avaliações.";

    private final AvaliacaoRepository avaliacoes;
    private final DisciplinaRepository disciplinas;
    private final AcessoDisciplina acesso;
    private final AvaliacaoMapper mapper;

    public AvaliacaoServiceImpl(AvaliacaoRepository avaliacoes,
                               DisciplinaRepository disciplinas,
                               AcessoDisciplina acesso,
                               AvaliacaoMapper mapper) {
        this.avaliacoes = avaliacoes;
        this.disciplinas = disciplinas;
        this.acesso = acesso;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public AvaliacaoResponseDTO cadastrar(AvaliacaoRequestDTO dto) {
        validar(dto);
        Disciplina disciplina = disciplinas.findById(dto.disciplinaId())
                .orElseThrow(() -> new DisciplinaNaoEncontradaException(dto.disciplinaId()));  //404
        acesso.exigirParticipanteAceito(disciplina.getId(), SEM_ACESSO);
        // A referência garante que a avaliação pertença a uma disciplina existente.
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setDisciplina(disciplina);
        avaliacao.setDescricao(dto.descricao().trim());
        avaliacao.setData(dto.data());
        avaliacao.setTipo(dto.tipo());
        return mapper.toDto(avaliacoes.save(avaliacao));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvaliacaoResponseDTO> listarPorDisciplina(UUID disciplinaId) {
        if (disciplinaId == null) {
            throw new RequisicaoInvalidaException("O id da disciplina é obrigatório.");
        }
        // Diferencia uma disciplina sem avaliações de uma disciplina inexistente.
        if (!disciplinas.existsById(disciplinaId)) {
            throw new DisciplinaNaoEncontradaException(disciplinaId);
        }
        acesso.exigirParticipanteAceito(disciplinaId, SEM_ACESSO);
        return mapper.toDtoList(avaliacoes.findByDisciplinaIdOrderByDataAscIdAsc(disciplinaId));
    }

    private void validar(AvaliacaoRequestDTO dto) {
        if (dto.disciplinaId() == null) {
            throw new RequisicaoInvalidaException("O id da disciplina é obrigatório.");
        }
        if (dto.descricao() == null || dto.descricao().isBlank() || dto.descricao().trim().length() > 500) {
            throw new RequisicaoInvalidaException("Informe uma descrição de até 500 caracteres.");
        }
        if (dto.data() == null) {
            throw new RequisicaoInvalidaException("A data da avaliação é obrigatória.");
        }
        if (dto.tipo() == null) {
            throw new RequisicaoInvalidaException("O tipo da avaliação é obrigatório: PROVA ou ENTREGA.");
        }
    }

}
