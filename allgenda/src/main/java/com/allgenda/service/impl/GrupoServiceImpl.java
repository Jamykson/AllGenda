package com.allgenda.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.allgenda.exception.GrupoNaoEncontradoException;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.framework.acesso.AcessoAoGrupo;
import com.allgenda.framework.acesso.Acao;
import com.allgenda.framework.acesso.PoliticaDeAcesso;
import com.allgenda.framework.extensao.DadosDoGrupo;
import com.allgenda.framework.extensao.RegistroDeTipos;
import com.allgenda.model.Grupo;
import com.allgenda.model.Participacao;
import com.allgenda.model.StatusParticipacao;
import com.allgenda.repository.GrupoRepository;
import com.allgenda.repository.ImagemAnotacaoRepository;
import com.allgenda.repository.ImagemEncontroRepository;
import com.allgenda.repository.ParticipacaoRepository;
import com.allgenda.repository.RecorrenciaRepository;
import com.allgenda.service.GrupoService;
import com.allgenda.service.RemocaoArquivos;
import com.allgenda.service.UsuarioAutenticado;

@Service
public class GrupoServiceImpl implements GrupoService {

    private static final int TAMANHO_MAXIMO_DESCRICAO = 500;

    private final GrupoRepository repository;
    private final ParticipacaoRepository participacaoRepository;
    private final RecorrenciaRepository recorrenciaRepository;
    private final ImagemAnotacaoRepository imagemRepository;
    private final ImagemEncontroRepository imagemEncontroRepository;
    private final RemocaoArquivos remocaoArquivos;
    private final UsuarioAutenticado usuarioAutenticado;
    private final AcessoAoGrupo acesso;
    private final PoliticaDeAcesso politica;
    private final RegistroDeTipos registro;

    public GrupoServiceImpl(GrupoRepository repository,
                            ParticipacaoRepository participacaoRepository,
                            RecorrenciaRepository recorrenciaRepository,
                            ImagemAnotacaoRepository imagemRepository,
                            ImagemEncontroRepository imagemEncontroRepository,
                            RemocaoArquivos remocaoArquivos,
                            UsuarioAutenticado usuarioAutenticado,
                            AcessoAoGrupo acesso,
                            PoliticaDeAcesso politica,
                            RegistroDeTipos registro) {
        this.repository = repository;
        this.participacaoRepository = participacaoRepository;
        this.recorrenciaRepository = recorrenciaRepository;
        this.imagemRepository = imagemRepository;
        this.imagemEncontroRepository = imagemEncontroRepository;
        this.remocaoArquivos = remocaoArquivos;
        this.usuarioAutenticado = usuarioAutenticado;
        this.acesso = acesso;
        this.politica = politica;
        this.registro = registro;
    }

    // Fluxo fixo: valida o que é comum, pede à fábrica do tipo a subclasse da aplicação, preenche os campos
    // comuns e grava o grupo junto com a participação de quem o criou.
    @Override
    @Transactional
    public Grupo criar(String tipo, DadosDoGrupo dados) {
        if (dados.nome() == null || dados.nome().isBlank()) {
            throw new RequisicaoInvalidaException("O nome do grupo é obrigatório.");
        }
        String nome = dados.nome().trim();
        String descricao = normalizarDescricao(dados.descricao());

        Grupo grupo = registro.fabricaDeGrupo(tipo).criar(dados); // tipo desconhecido: 400
        grupo.setNome(nome);
        grupo.setDescricao(descricao);

        // quem cria o grupo recebe o papel de criador definido pela política, com a participação já aceita
        Participacao participacao = new Participacao();
        participacao.setUsuario(usuarioAutenticado.obter());
        participacao.setPapel(politica.papelDoCriador());
        participacao.setStatus(StatusParticipacao.ACEITO);
        Grupo salvo = repository.save(grupo);
        salvo.addParticipacao(participacao);
        // A associação não usa cascata: grupo e participação do criador são gravados na mesma transação.
        participacaoRepository.save(participacao);
        return salvo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Grupo> listarDoUsuario() {
        // só os grupos em que a pessoa participa com status ACEITO (qualquer papel)
        return participacaoRepository
            .findByUsuarioIdAndStatus(usuarioAutenticado.id(), StatusParticipacao.ACEITO)
            .stream()
            .map(Participacao::getGrupo)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Grupo buscarPorId(UUID id) {
        Grupo grupo = repository.findById(id)
            .orElseThrow(() -> new GrupoNaoEncontradoException(id));   // 404
        acesso.exigir(Acao.VER_CONTEUDO, id, "Somente participantes do grupo têm acesso a ele."); // 403
        return grupo;
    }

    @Override
    @Transactional
    public Grupo atualizarDescricao(UUID id, String descricao) {
        Grupo grupo = repository.findById(id)
            .orElseThrow(() -> new GrupoNaoEncontradoException(id));       // 404
        acesso.exigir(Acao.EDITAR_GRUPO, id, "Você não tem permissão para editar o grupo.");       // 403

        grupo.setDescricao(normalizarDescricao(descricao));               // 400 se passar do limite
        return repository.save(grupo);
    }

    @Override
    @Transactional
    public void excluir(UUID id) {
        Grupo grupo = repository.findById(id)
            .orElseThrow(() -> new GrupoNaoEncontradoException(id));   // 404
        acesso.exigir(Acao.EXCLUIR_GRUPO, id, "Você não tem permissão para excluir o grupo."); // 403, antes de apagar qualquer coisa

        // a cascata grupo -> encontros -> anotações -> imagens (e encontros -> imagens avulsas) apaga os registros,
        // mas não os arquivos; as chaves são lidas antes e os arquivos só saem do disco depois que a exclusão é confirmada
        List<String> chavesDasImagens = new ArrayList<>(imagemRepository.findChavesArmazenamentoByGrupoId(id));
        chavesDasImagens.addAll(imagemEncontroRepository.findChavesArmazenamentoByGrupoId(id));
        // Remove os vínculos antes do grupo para respeitar as FKs, sem composição JPA.
        participacaoRepository.deleteByGrupoId(id);
        recorrenciaRepository.deleteByGrupoId(id);
        repository.delete(grupo);
        remocaoArquivos.removerAoConfirmar(chavesDasImagens);
    }

    // sem espaços nas pontas; em branco, o grupo fica sem descrição
    private static String normalizarDescricao(String descricao) {
        String semEspacos = descricao == null ? null : descricao.trim();
        if (semEspacos != null && semEspacos.length() > TAMANHO_MAXIMO_DESCRICAO) {
            throw new RequisicaoInvalidaException("A descrição deve ter no máximo " + TAMANHO_MAXIMO_DESCRICAO + " caracteres.");
        }
        return semEspacos == null || semEspacos.isBlank() ? null : semEspacos;
    }
}
