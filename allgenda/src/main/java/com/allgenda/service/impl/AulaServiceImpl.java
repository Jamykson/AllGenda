package com.allgenda.service.impl;

import com.allgenda.exception.EncontroNaoEncontradoException;
import com.allgenda.exception.GrupoNaoEncontradoException;
import com.allgenda.model.Aula;
import com.allgenda.model.Encontro;
import com.allgenda.model.Grupo;
import com.allgenda.repository.EncontroRepository;
import com.allgenda.repository.GrupoRepository;
import com.allgenda.repository.ImagemAnotacaoRepository;
import com.allgenda.repository.ImagemEncontroRepository;
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

    private final EncontroRepository encontroRepository;
    private final GrupoRepository grupoRepository;
    private final AcessoDisciplina acesso;
    private final ImagemAnotacaoRepository imagemAnotacaoRepository;
    private final ImagemEncontroRepository imagemEncontroRepository;
    private final RemocaoArquivos remocaoArquivos;

    public AulaServiceImpl(EncontroRepository encontroRepository, GrupoRepository grupoRepository,
                           AcessoDisciplina acesso, ImagemAnotacaoRepository imagemAnotacaoRepository,
                           ImagemEncontroRepository imagemEncontroRepository, RemocaoArquivos remocaoArquivos) {
        this.imagemAnotacaoRepository = imagemAnotacaoRepository;
        this.imagemEncontroRepository = imagemEncontroRepository;
        this.remocaoArquivos = remocaoArquivos;
        this.encontroRepository = encontroRepository;
        this.grupoRepository = grupoRepository;
        this.acesso = acesso;
    }

    @Override
    public Encontro cadastrar(DadosDoEncontro dados) {
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

        Grupo disciplina = grupoRepository.findById(dados.grupoId())           // a disciplina informada existe?
                .orElseThrow(() -> new GrupoNaoEncontradoException(dados.grupoId()));
        acesso.exigirParticipanteAceito(disciplina.getId());   // 403

        Encontro aula = new Aula();
        aula.setData(dados.data());
        aula.setHorario(dados.horario());
        aula.setTopico(dados.topico());
        aula.setGrupo(disciplina);

        return encontroRepository.save(aula);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Encontro> listarPorDisciplina(UUID disciplinaId) {
        if (!grupoRepository.existsById(disciplinaId)) {                // 404
            throw new GrupoNaoEncontradoException(disciplinaId);
        }
        acesso.exigirParticipanteAceito(disciplinaId);                       // 403
        return encontroRepository.findByGrupoId(disciplinaId);
    }

    @Override
    @Transactional(readOnly = true)
    public Encontro buscarPorId(UUID id) {
        Encontro aula = encontroRepository.findById(id)
                .orElseThrow(() -> new EncontroNaoEncontradoException(id));      // 404
        acesso.exigirParticipanteAceito(aula.getGrupo().getId());       // 403
        return aula;
    }

    @Override
    @Transactional
    public Encontro atualizar(UUID id, AtualizacaoDeEncontro dto) {
        Encontro aula = encontroRepository.findById(id)
                .orElseThrow(() -> new EncontroNaoEncontradoException(id));                       // 404
        acesso.exigirDono(aula.getGrupo().getId(), "Somente o dono pode editar a aula.");  // 403

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
        return encontroRepository.save(aula);
    }

    @Override
    @Transactional
    public void excluir(UUID id) {
        Encontro aula = encontroRepository.findById(id)
                .orElseThrow(() -> new EncontroNaoEncontradoException(id));                         // 404
        acesso.exigirDono(aula.getGrupo().getId(), "Somente o dono pode excluir a aula.");  // 403, antes de apagar qualquer coisa

        // a cascata aula -> anotações -> imagens (e aula -> imagens avulsas) apaga os registros, mas não os arquivos;
        // as chaves são lidas antes e os arquivos só saem do disco depois que a exclusão é confirmada
        List<String> chaves = new ArrayList<>(imagemAnotacaoRepository.findChavesArmazenamentoByEncontroId(id));
        chaves.addAll(imagemEncontroRepository.findChavesArmazenamentoByEncontroId(id));
        encontroRepository.delete(aula);
        remocaoArquivos.removerAoConfirmar(chaves);
    }
}
