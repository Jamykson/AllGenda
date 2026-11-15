package com.allgenda.service.impl;

import com.allgenda.dto.request.AulaRequestDTO;
import com.allgenda.dto.request.AulaUpdateDTO;
import com.allgenda.dto.response.AulaResponseDTO;
import com.allgenda.exception.AulaNaoEncontradaException;
import com.allgenda.exception.DisciplinaNaoEncontradaException;
import com.allgenda.mapper.AulaMapper;
import com.allgenda.model.Aula;
import com.allgenda.model.Disciplina;
import com.allgenda.repository.AulaRepository;
import com.allgenda.repository.DisciplinaRepository;
import com.allgenda.repository.ImagemAnotacaoRepository;
import com.allgenda.repository.ImagemAulaRepository;
import com.allgenda.service.RemocaoArquivos;
import com.allgenda.service.AcessoDisciplina;
import com.allgenda.service.AulaService;
import org.springframework.stereotype.Service;
import com.allgenda.exception.RequisicaoInvalidaException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AulaServiceImpl implements AulaService {

    private final AulaRepository aulaRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final AulaMapper mapper;
    private final AcessoDisciplina acesso;
    private final ImagemAnotacaoRepository imagemAnotacaoRepository;
    private final ImagemAulaRepository imagemAulaRepository;
    private final RemocaoArquivos remocaoArquivos;

    public AulaServiceImpl(AulaRepository aulaRepository, DisciplinaRepository disciplinaRepository, AulaMapper mapper,
                           AcessoDisciplina acesso, ImagemAnotacaoRepository imagemAnotacaoRepository,
                           ImagemAulaRepository imagemAulaRepository, RemocaoArquivos remocaoArquivos) {
        this.imagemAnotacaoRepository = imagemAnotacaoRepository;
        this.imagemAulaRepository = imagemAulaRepository;
        this.remocaoArquivos = remocaoArquivos;
        this.aulaRepository = aulaRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.mapper = mapper;
        this.acesso = acesso;
    }

    @Override
    public AulaResponseDTO cadastrar(AulaRequestDTO dto) {
        if (dto.disciplinaId() == null) {
            throw new RequisicaoInvalidaException("O id da disciplina é obrigatório.");
        }
        if (dto.data() == null) {
            throw new RequisicaoInvalidaException("A data da aula é obrigatória.");
        }
        if (dto.horario() == null || dto.horario().isBlank()) {
            throw new RequisicaoInvalidaException("O horário da aula é obrigatório.");
        }
        if (dto.topico() == null || dto.topico().isBlank()) {
            throw new RequisicaoInvalidaException("O tópico da aula é obrigatório.");
        }

        Disciplina disciplina = disciplinaRepository.findById(dto.disciplinaId())           // checar se o disciplinaId recebido é válido (tal disciplina existe?)
                .orElseThrow(() -> new DisciplinaNaoEncontradaException(dto.disciplinaId()));
        acesso.exigirParticipanteAceito(disciplina.getId());   // 403

        Aula aula = new Aula();
        aula.setData(dto.data());
        aula.setHorario(dto.horario());
        aula.setTopico(dto.topico());
        aula.setDisciplina(disciplina);

        Aula salva = aulaRepository.save(aula);
        return mapper.toDto(salva);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AulaResponseDTO> listarPorDisciplina(UUID disciplinaId) {
        if (!disciplinaRepository.existsById(disciplinaId)) {                // 404
            throw new DisciplinaNaoEncontradaException(disciplinaId);
        }
        acesso.exigirParticipanteAceito(disciplinaId);                       // 403
        return mapper.toDtoList(aulaRepository.findByDisciplinaId(disciplinaId));
    }

    @Override
    @Transactional(readOnly = true)
    public AulaResponseDTO buscarPorId(UUID id) {
        Aula aula = aulaRepository.findById(id)
                .orElseThrow(() -> new AulaNaoEncontradaException(id));      // 404
        acesso.exigirParticipanteAceito(aula.getDisciplina().getId());       // 403
        return mapper.toDto(aula);
    }

    @Override
    @Transactional
    public AulaResponseDTO atualizar(UUID id, AulaUpdateDTO dto) {
        Aula aula = aulaRepository.findById(id)
                .orElseThrow(() -> new AulaNaoEncontradaException(id));                       // 404
        acesso.exigirDono(aula.getDisciplina().getId(), "Somente o dono pode editar a aula.");  // 403

        if (dto == null) {
            throw new RequisicaoInvalidaException("Corpo da requisição é obrigatório.");       // 400
        }
        if (dto.horario() != null && dto.horario().isBlank()) {
            throw new RequisicaoInvalidaException("O horário da aula não pode ficar em branco.");
        }
        if (dto.topico() != null && dto.topico().isBlank()) {
            throw new RequisicaoInvalidaException("O tópico da aula não pode ficar em branco.");
        }

        // o que veio nulo continua como estava
        if (dto.data() != null) aula.setData(dto.data());
        if (dto.horario() != null) aula.setHorario(dto.horario().trim());
        if (dto.topico() != null) aula.setTopico(dto.topico().trim());
        if (dto.cancelada() != null) aula.setCancelada(dto.cancelada());
        return mapper.toDto(aulaRepository.save(aula));
    }

    @Override
    @Transactional
    public void excluir(UUID id) {
        Aula aula = aulaRepository.findById(id)
                .orElseThrow(() -> new AulaNaoEncontradaException(id));                         // 404
        acesso.exigirDono(aula.getDisciplina().getId(), "Somente o dono pode excluir a aula.");  // 403, antes de apagar qualquer coisa

        // a cascata aula -> anotações -> imagens (e aula -> imagens avulsas) apaga os registros, mas não os arquivos;
        // as chaves são lidas antes e os arquivos só saem do disco depois que a exclusão é confirmada
        List<String> chaves = new ArrayList<>(imagemAnotacaoRepository.findChavesArmazenamentoByAulaId(id));
        chaves.addAll(imagemAulaRepository.findChavesArmazenamentoByAulaId(id));
        aulaRepository.delete(aula);
        remocaoArquivos.removerAoConfirmar(chaves);
    }
}
