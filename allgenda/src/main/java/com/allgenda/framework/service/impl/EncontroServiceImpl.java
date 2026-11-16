package com.allgenda.framework.service.impl;

import com.allgenda.framework.exception.EncontroNaoEncontradoException;
import com.allgenda.framework.exception.GrupoNaoEncontradoException;
import com.allgenda.framework.exception.RequisicaoInvalidaException;
import com.allgenda.framework.acesso.AcessoAoGrupo;
import com.allgenda.framework.acesso.Acao;
import com.allgenda.framework.extensao.DadosDoEncontro;
import com.allgenda.framework.extensao.FabricaDeEncontro;
import com.allgenda.framework.extensao.RegistroDeTipos;
import com.allgenda.framework.model.Encontro;
import com.allgenda.framework.model.Grupo;
import com.allgenda.framework.repository.EncontroRepository;
import com.allgenda.framework.repository.GrupoRepository;
import com.allgenda.framework.repository.ImagemAnotacaoRepository;
import com.allgenda.framework.repository.ImagemEncontroRepository;
import com.allgenda.framework.service.AtualizacaoDeEncontro;
import com.allgenda.framework.service.EncontroService;
import com.allgenda.framework.service.RemocaoArquivos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EncontroServiceImpl implements EncontroService {

    private static final String SEM_ACESSO = "Somente participantes do grupo têm acesso aos encontros dele.";

    private final EncontroRepository encontroRepository;
    private final GrupoRepository grupoRepository;
    private final AcessoAoGrupo acesso;
    private final ImagemAnotacaoRepository imagemAnotacaoRepository;
    private final ImagemEncontroRepository imagemEncontroRepository;
    private final RemocaoArquivos remocaoArquivos;
    private final RegistroDeTipos registro;

    public EncontroServiceImpl(EncontroRepository encontroRepository, GrupoRepository grupoRepository,
                               AcessoAoGrupo acesso, ImagemAnotacaoRepository imagemAnotacaoRepository,
                               ImagemEncontroRepository imagemEncontroRepository, RemocaoArquivos remocaoArquivos,
                               RegistroDeTipos registro) {
        this.encontroRepository = encontroRepository;
        this.grupoRepository = grupoRepository;
        this.acesso = acesso;
        this.imagemAnotacaoRepository = imagemAnotacaoRepository;
        this.imagemEncontroRepository = imagemEncontroRepository;
        this.remocaoArquivos = remocaoArquivos;
        this.registro = registro;
    }

    // Fluxo fixo: valida o que é comum, confere o grupo e a permissão, pede à fábrica do tipo a subclasse
    // da aplicação e preenche os campos comuns.
    @Override
    public Encontro criar(String tipo, DadosDoEncontro dados) {
        if (dados.grupoId() == null) {
            throw new RequisicaoInvalidaException("O id do grupo é obrigatório.");
        }
        if (dados.data() == null) {
            throw new RequisicaoInvalidaException("A data do encontro é obrigatória.");
        }
        if (dados.horario() == null || dados.horario().isBlank()) {
            throw new RequisicaoInvalidaException("O horário do encontro é obrigatório.");
        }
        if (dados.topico() == null || dados.topico().isBlank()) {
            throw new RequisicaoInvalidaException("O tópico do encontro é obrigatório.");
        }
        FabricaDeEncontro fabrica = registro.fabricaDeEncontro(tipo);    // 400 se o tipo não existir

        Grupo grupo = grupoRepository.findById(dados.grupoId())
                .orElseThrow(() -> new GrupoNaoEncontradoException(dados.grupoId()));   // 404
        acesso.exigir(Acao.CRIAR_ENCONTRO, grupo.getId(), "Você não tem permissão para criar encontros neste grupo."); // 403

        Encontro encontro = fabrica.criar(dados, grupo);
        encontro.setData(dados.data());
        encontro.setHorario(dados.horario());
        encontro.setTopico(dados.topico());
        encontro.setGrupo(grupo);
        return encontroRepository.save(encontro);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Encontro> listarPorGrupo(UUID grupoId) {
        if (!grupoRepository.existsById(grupoId)) {                // 404
            throw new GrupoNaoEncontradoException(grupoId);
        }
        acesso.exigir(Acao.VER_CONTEUDO, grupoId, SEM_ACESSO);    // 403
        return encontroRepository.findByGrupoId(grupoId);
    }

    @Override
    @Transactional(readOnly = true)
    public Encontro buscarPorId(UUID id) {
        Encontro encontro = buscar(id);                                           // 404
        acesso.exigir(Acao.VER_CONTEUDO, encontro.getGrupo().getId(), SEM_ACESSO);  // 403
        return encontro;
    }

    @Override
    @Transactional
    public Encontro atualizar(UUID id, AtualizacaoDeEncontro atualizacao) {
        Encontro encontro = buscar(id);                                           // 404
        UUID grupoId = encontro.getGrupo().getId();
        acesso.exigir(Acao.EDITAR_ENCONTRO, grupoId, "Você não tem permissão para editar o encontro.");  // 403
        // o mesmo PATCH também cancela ou reativa o encontro, que é uma ação à parte na política
        if (atualizacao != null && atualizacao.cancelada() != null) {
            acesso.exigir(Acao.CANCELAR_ENCONTRO, grupoId, "Você não tem permissão para cancelar o encontro.");
        }

        if (atualizacao == null) {
            throw new RequisicaoInvalidaException("Corpo da requisição é obrigatório.");       // 400
        }
        if (atualizacao.horario() != null && atualizacao.horario().isBlank()) {
            throw new RequisicaoInvalidaException("O horário do encontro não pode ficar em branco.");
        }
        if (atualizacao.topico() != null && atualizacao.topico().isBlank()) {
            throw new RequisicaoInvalidaException("O tópico do encontro não pode ficar em branco.");
        }

        // o que veio nulo continua como estava
        if (atualizacao.data() != null) encontro.setData(atualizacao.data());
        if (atualizacao.horario() != null) encontro.setHorario(atualizacao.horario().trim());
        if (atualizacao.topico() != null) encontro.setTopico(atualizacao.topico().trim());
        if (atualizacao.cancelada() != null) encontro.setCancelada(atualizacao.cancelada());
        return encontroRepository.save(encontro);
    }

    @Override
    @Transactional
    public void excluir(UUID id) {
        Encontro encontro = buscar(id);                                           // 404
        acesso.exigir(Acao.EXCLUIR_ENCONTRO, encontro.getGrupo().getId(), "Você não tem permissão para excluir o encontro."); // 403, antes de apagar qualquer coisa

        // a cascata encontro -> anotações -> imagens (e encontro -> imagens avulsas) apaga os registros, mas não os
        // arquivos; as chaves são lidas antes e os arquivos só saem do disco depois que a exclusão é confirmada
        List<String> chaves = new ArrayList<>(imagemAnotacaoRepository.findChavesArmazenamentoByEncontroId(id));
        chaves.addAll(imagemEncontroRepository.findChavesArmazenamentoByEncontroId(id));
        encontroRepository.delete(encontro);
        remocaoArquivos.removerAoConfirmar(chaves);
    }

    private Encontro buscar(UUID id) {
        return encontroRepository.findById(id).orElseThrow(() -> new EncontroNaoEncontradoException(id));
    }
}
