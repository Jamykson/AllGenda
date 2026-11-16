package com.allgenda.service.impl;

import com.allgenda.exception.AulaNaoEncontradaException;
import com.allgenda.exception.DisciplinaNaoEncontradaException;
import com.allgenda.model.Aula;
import com.allgenda.model.Disciplina;
import com.allgenda.repository.AulaRepository;
import com.allgenda.repository.DisciplinaRepository;
import com.allgenda.repository.ImagemAnotacaoRepository;
import com.allgenda.repository.ImagemAulaRepository;
import com.allgenda.service.RemocaoArquivos;
import com.allgenda.service.AcessoDisciplina;
import com.allgenda.service.AtualizacaoDeEncontro;
import com.allgenda.service.AulaService;
import com.allgenda.service.DadosDoEncontro;
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
    private final AcessoDisciplina acesso;
    private final ImagemAnotacaoRepository imagemAnotacaoRepository;
    private final ImagemAulaRepository imagemAulaRepository;
    private final RemocaoArquivos remocaoArquivos;

    public AulaServiceImpl(AulaRepository aulaRepository, DisciplinaRepository disciplinaRepository,
                           AcessoDisciplina acesso, ImagemAnotacaoRepository imagemAnotacaoRepository,
                           ImagemAulaRepository imagemAulaRepository, RemocaoArquivos remocaoArquivos) {
        this.imagemAnotacaoRepository = imagemAnotacaoRepository;
        this.imagemAulaRepository = imagemAulaRepository;
        this.remocaoArquivos = remocaoArquivos;
        this.aulaRepository = aulaRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.acesso = acesso;
    }

    @Override
    public Aula cadastrar(DadosDoEncontro dados) {
        if (dados.grupoId() == null) {
            throw new RequisicaoInvalidaException("O id da disciplina é obrigatório.");
        }
        if (dados.data() == null) {
            throw new RequisicaoInvalidaException("A data da aula é obrigatória.");
        }
        if (dados.horario() == null || dados.horario().isBlank()) {
            throw new RequisicaoInvalidaException("O horário da aula é obrigatório.");
        }
        if (dados.topico() == null || dados.topico().isBlank()) {
            throw new RequisicaoInvalidaException("O tópico da aula é obrigatório.");
        }

        Disciplina disciplina = disciplinaRepository.findById(dados.grupoId())           // a disciplina informada existe?
                .orElseThrow(() -> new DisciplinaNaoEncontradaException(dados.grupoId()));
        acesso.exigirParticipanteAceito(disciplina.getId());   // 403

        Aula aula = new Aula();
        aula.setData(dados.data());
        aula.setHorario(dados.horario());
        aula.setTopico(dados.topico());
        aula.setDisciplina(disciplina);

        return aulaRepository.save(aula);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Aula> listarPorDisciplina(UUID disciplinaId) {
        if (!disciplinaRepository.existsById(disciplinaId)) {                // 404
            throw new DisciplinaNaoEncontradaException(disciplinaId);
        }
        acesso.exigirParticipanteAceito(disciplinaId);                       // 403
        return aulaRepository.findByDisciplinaId(disciplinaId);
    }

    @Override
    @Transactional(readOnly = true)
    public Aula buscarPorId(UUID id) {
        Aula aula = aulaRepository.findById(id)
                .orElseThrow(() -> new AulaNaoEncontradaException(id));      // 404
        acesso.exigirParticipanteAceito(aula.getDisciplina().getId());       // 403
        return aula;
    }

    @Override
    @Transactional
    public Aula atualizar(UUID id, AtualizacaoDeEncontro dto) {
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
        return aulaRepository.save(aula);
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
