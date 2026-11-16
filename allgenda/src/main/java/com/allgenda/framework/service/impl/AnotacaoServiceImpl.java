package com.allgenda.framework.service.impl;
import com.allgenda.framework.exception.AcessoNegadoException;
import com.allgenda.framework.acesso.AcessoAoGrupo;
import com.allgenda.framework.acesso.Acao;
import com.allgenda.framework.exception.AnotacaoNaoEncontradaException;
import com.allgenda.framework.exception.EncontroNaoEncontradoException;
import com.allgenda.framework.exception.GrupoNaoEncontradoException;
import com.allgenda.framework.exception.RequisicaoInvalidaException;
import com.allgenda.framework.model.Anotacao;
import com.allgenda.framework.model.Encontro;
import com.allgenda.framework.model.ImagemAnotacao;
import com.allgenda.framework.model.StatusParticipacao;
import com.allgenda.framework.model.VisibilidadeAnotacao;
import com.allgenda.framework.repository.ParticipacaoRepository;
import com.allgenda.framework.repository.AnotacaoRepository;
import com.allgenda.framework.repository.EncontroRepository;
import com.allgenda.framework.repository.GrupoRepository;
import com.allgenda.framework.service.AnotacaoService;
import com.allgenda.framework.service.NovaAnotacao;
import com.allgenda.framework.service.RemocaoArquivos;
import com.allgenda.framework.service.TagService;
import com.allgenda.framework.service.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AnotacaoServiceImpl implements AnotacaoService {

    private static final int TAMANHO_MAXIMO_TITULO = 150;
    private static final String SEM_ACESSO = "Somente participantes do grupo acessam as anotações dele.";

    private final AnotacaoRepository anotacaoRepository;
    private final EncontroRepository encontroRepository;
    private final GrupoRepository grupoRepository;
    private final ParticipacaoRepository participacaoRepository;
    private final UsuarioAutenticado usuarioAutenticado;
    private final AcessoAoGrupo acesso;
    private final TagService tagService;
    private final RemocaoArquivos remocaoArquivos;

    public AnotacaoServiceImpl(AnotacaoRepository anotacaoRepository,
                                EncontroRepository encontroRepository,
                                GrupoRepository grupoRepository,
                                ParticipacaoRepository participacaoRepository,
                                UsuarioAutenticado usuarioAutenticado,
                                AcessoAoGrupo acesso,
                                TagService tagService,
                                RemocaoArquivos remocaoArquivos) {
        this.anotacaoRepository = anotacaoRepository;
        this.encontroRepository = encontroRepository;
        this.grupoRepository = grupoRepository;
        this.participacaoRepository = participacaoRepository;
        this.usuarioAutenticado = usuarioAutenticado;
        this.acesso = acesso;
        this.tagService = tagService;
        this.remocaoArquivos = remocaoArquivos;
    }

    @Override
    @Transactional // anotação e tags novas são salvas juntas: se algo falhar, nada fica salvo pela metade
    public Anotacao cadastrar(NovaAnotacao nova) {
        if (nova.encontroId() == null) {
            throw new RequisicaoInvalidaException("O id da aula é obrigatório.");
        }
        String titulo = normalizarTitulo(nova.titulo());
        String conteudo = normalizarConteudo(nova.conteudo());
        exigirTituloOuConteudo(titulo, conteudo);

        Encontro aula = encontroRepository.findById(nova.encontroId())
                .orElseThrow(() -> new EncontroNaoEncontradoException(nova.encontroId()));   // 404
        acesso.exigir(Acao.CRIAR_ANOTACAO, aula.getGrupo().getId(), SEM_ACESSO);   // 403

        Anotacao anotacao = new Anotacao();
        anotacao.setTitulo(titulo);
        anotacao.setConteudo(conteudo);
        anotacao.setDataCriacao(LocalDateTime.now());
        anotacao.setEncontro(aula);
        anotacao.setAutor(usuarioAutenticado.obter());
        anotacao.setVisibilidade(nova.visibilidade() == null ? VisibilidadeAnotacao.GRUPO : nova.visibilidade());
        tagService.buscarOuCriar(aula.getGrupo(), nova.tags()).forEach(anotacao::addTag); // tags são opcionais

        return anotacaoRepository.save(anotacao);
    }

    @Override
    @Transactional
    public Anotacao associarTags(UUID anotacaoId, List<String> nomesTags) {
        if (nomesTags == null || nomesTags.stream().allMatch(nome -> nome == null || nome.isBlank())) {
            throw new RequisicaoInvalidaException("Informe ao menos uma tag.");
        }

        Anotacao anotacao = buscarVisivel(anotacaoId);
        // qualquer participante que possa anotar pode acrescentar tags, não só o autor (decisão do grupo, #64)
        acesso.exigir(Acao.CRIAR_ANOTACAO, anotacao.getEncontro().getGrupo().getId(), SEM_ACESSO);

        // as tags são acrescentadas às existentes; como tags é um Set, uma tag já associada não é duplicada
        tagService.buscarOuCriar(anotacao.getEncontro().getGrupo(), nomesTags).forEach(anotacao::addTag);

        return anotacaoRepository.save(anotacao);
    }

    @Override
    @Transactional
    public Anotacao removerTag(UUID anotacaoId, String nomeTag) {
        if (nomeTag == null || nomeTag.isBlank()) {
            throw new RequisicaoInvalidaException("O nome da tag é obrigatório.");
        }

        Anotacao anotacao = buscarVisivel(anotacaoId);
        // como acrescentar, remover é permitido a qualquer participante que possa anotar
        acesso.exigir(Acao.CRIAR_ANOTACAO, anotacao.getEncontro().getGrupo().getId(), SEM_ACESSO);

        // a busca ignora maiúsculas, como a criação de tags; uma tag que a anotação não tem não é erro
        String nome = nomeTag.strip();
        anotacao.getTags().stream()
                .filter(tag -> tag.getNome().equalsIgnoreCase(nome))
                .toList()
                .forEach(anotacao::removeTag);

        return anotacaoRepository.save(anotacao);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Anotacao> listarPorAula(UUID aulaId) {
        Encontro aula = encontroRepository.findById(aulaId)
                .orElseThrow(() -> new EncontroNaoEncontradoException(aulaId));  
        acesso.exigir(Acao.VER_CONTEUDO, aula.getGrupo().getId(), SEM_ACESSO);     //403
        return soVisiveis(anotacaoRepository.findByEncontroIdOrderByDataCriacaoAsc(aulaId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Anotacao> listarPorDisciplina(UUID disciplinaId, String tag) {
        if (!grupoRepository.existsById(disciplinaId)) {    //404
            throw new GrupoNaoEncontradoException(disciplinaId);
        }
        acesso.exigir(Acao.VER_CONTEUDO, disciplinaId, SEM_ACESSO);
        if (tag == null || tag.isBlank()) {
            return soVisiveis(anotacaoRepository.findByEncontroGrupoIdOrderByEncontroDataAscDataCriacaoAsc(disciplinaId));
        }
        return soVisiveis(anotacaoRepository
                .findByEncontroGrupoIdAndTagsNomeIgnoreCaseOrderByEncontroDataAscDataCriacaoAsc(disciplinaId, tag.trim()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Anotacao> listarPorTag(String tag) {
        if (tag == null || tag.isBlank()) {
            throw new RequisicaoInvalidaException("O nome da tag é obrigatório.");
        }
        // sem disciplina informada, a busca vale só para as disciplinas em que o usuário participa (ACEITO)
        List<UUID> disciplinasDoUsuario = participacaoRepository
                .findByUsuarioIdAndStatus(usuarioAutenticado.id(), StatusParticipacao.ACEITO)
                .stream()
                .map(participacao -> participacao.getGrupo().getId())
                .toList();
        if (disciplinasDoUsuario.isEmpty()) {
            return List.of();
        }
        // tag que não existe não é erro: só não há anotações com ela
        return soVisiveis(anotacaoRepository
                .findByTagsNomeIgnoreCaseAndEncontroGrupoIdInOrderByEncontroDataAscDataCriacaoAsc(tag.trim(), disciplinasDoUsuario));
    }

    // título em branco vira null; o limite acompanha a coluna titulo de Anotacao
    private static String normalizarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            return null;
        }
        String semEspacos = titulo.strip();
        if (semEspacos.length() > TAMANHO_MAXIMO_TITULO) {
            throw new RequisicaoInvalidaException("O título deve ter no máximo " + TAMANHO_MAXIMO_TITULO + " caracteres.");
        }
        return semEspacos;
    }

    // conteúdo em branco vira null; o texto informado é mantido como veio, com quebras de linha e recuos
    private static String normalizarConteudo(String conteudo) {
        return conteudo == null || conteudo.isBlank() ? null : conteudo;
    }

    private static void exigirTituloOuConteudo(String titulo, String conteudo) {
        if (titulo == null && conteudo == null) {
            throw new RequisicaoInvalidaException("Escreva um título ou um texto para a anotação.");
        }
    }

    // Busca a anotação pelo id; a privada de outra pessoa dá 404, como se não existisse, para que ninguém
    // descubra pelo status que ela existe. Toda leitura ou alteração de uma anotação por id passa por aqui.
    private Anotacao buscarVisivel(UUID id) {
        Anotacao anotacao = anotacaoRepository.findById(id)
                .orElseThrow(() -> new AnotacaoNaoEncontradaException(id));
        if (!anotacao.visivelPara(usuarioAutenticado.id())) {
            throw new AnotacaoNaoEncontradaException(id);
        }
        return anotacao;
    }

    // as listagens tiram as privadas de outras pessoas
    private List<Anotacao> soVisiveis(List<Anotacao> anotacoes) {
        UUID usuarioId = usuarioAutenticado.id();
        return anotacoes.stream().filter(anotacao -> anotacao.visivelPara(usuarioId)).toList();
    }

    // acao completa a mensagem de erro, ex.: "editar a anotação" -> "Somente o autor pode editar a anotação."
    private void exigirAutor(Anotacao anotacao, String acao) {
        if (!usuarioAutenticado.id().equals(anotacao.getAutor().getId())) {
            throw new AcessoNegadoException("Somente o autor pode " + acao + ".");
        }
    }

    @Override
    @Transactional
    public Anotacao editar(UUID id, String tituloInformado, String conteudoInformado) {
        String titulo = normalizarTitulo(tituloInformado);
        String conteudo = normalizarConteudo(conteudoInformado);
        exigirTituloOuConteudo(titulo, conteudo);

        Anotacao anotacao = buscarVisivel(id);
        exigirAutor(anotacao, "editar a anotação");

        anotacao.setTitulo(titulo);
        anotacao.setConteudo(conteudo);

        return anotacaoRepository.save(anotacao);
    }

    @Override
    @Transactional
    public Anotacao alterarVisibilidade(UUID id, VisibilidadeAnotacao visibilidade) {
        if (visibilidade == null) {
            throw new RequisicaoInvalidaException("Informe a visibilidade: GRUPO ou PRIVADA.");
        }
        Anotacao anotacao = buscarVisivel(id);
        exigirAutor(anotacao, "alterar a visibilidade da anotação");

        anotacao.setVisibilidade(visibilidade);
        return anotacaoRepository.save(anotacao);
    }

    @Override
    @Transactional
    public void excluir(UUID id) {
        Anotacao anotacao = buscarVisivel(id);
        exigirAutor(anotacao, "excluir a anotação");

        // as chaves são lidas antes, porque a cascata apaga os registros das imagens junto com a anotação
        List<String> chavesDasImagens = anotacao.getImagens().stream()
                .map(ImagemAnotacao::getChaveArmazenamento)
                .toList();

        // os vínculos em anotacao_tag saem junto, pois a anotação é dona da relação; as tags continuam existindo
        anotacaoRepository.delete(anotacao);
        remocaoArquivos.removerAoConfirmar(chavesDasImagens);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Anotacao> listarDosColegas(UUID disciplinaId) {
        if (!grupoRepository.existsById(disciplinaId)) {    // 404
            throw new GrupoNaoEncontradoException(disciplinaId);
        }
        acesso.exigir(Acao.VER_CONTEUDO, disciplinaId, SEM_ACESSO);    // 403
        return soVisiveis(anotacaoRepository
                .findByEncontroGrupoIdAndAutorIdNotOrderByEncontroDataAscDataCriacaoAsc(disciplinaId, usuarioAutenticado.id()));
    }

}