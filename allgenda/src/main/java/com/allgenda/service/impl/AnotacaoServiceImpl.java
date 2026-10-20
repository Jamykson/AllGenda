package com.allgenda.service.impl;
import com.allgenda.dto.request.AnotacaoUpdateDTO;

import com.allgenda.dto.request.AnotacaoRequestDTO;
import com.allgenda.dto.response.AnotacaoResponseDTO;
import com.allgenda.exception.AcessoNegadoException;
import com.allgenda.exception.AnotacaoNaoEncontradaException;
import com.allgenda.exception.AulaNaoEncontradaException;
import com.allgenda.exception.DisciplinaNaoEncontradaException;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.mapper.AnotacaoMapper;
import com.allgenda.model.Anotacao;
import com.allgenda.model.Aula;
import com.allgenda.model.ImagemAnotacao;
import com.allgenda.model.StatusParticipacao;
import com.allgenda.repository.ParticipacaoRepository;
import com.allgenda.repository.AnotacaoRepository;
import com.allgenda.repository.AulaRepository;
import com.allgenda.repository.DisciplinaRepository;
import com.allgenda.service.AnotacaoService;
import com.allgenda.service.RemocaoArquivos;
import com.allgenda.service.TagService;
import com.allgenda.service.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AnotacaoServiceImpl implements AnotacaoService {

    private static final int TAMANHO_MAXIMO_TITULO = 150;

    private final AnotacaoRepository anotacaoRepository;
    private final AulaRepository aulaRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final ParticipacaoRepository participacaoRepository;
    private final UsuarioAutenticado usuarioAutenticado;
    private final TagService tagService;
    private final RemocaoArquivos remocaoArquivos;
    private final AnotacaoMapper mapper;

    public AnotacaoServiceImpl(AnotacaoRepository anotacaoRepository,
                                AulaRepository aulaRepository,
                                DisciplinaRepository disciplinaRepository,
                                ParticipacaoRepository participacaoRepository,
                                UsuarioAutenticado usuarioAutenticado,
                                TagService tagService,
                                RemocaoArquivos remocaoArquivos,
                                AnotacaoMapper mapper) {
        this.anotacaoRepository = anotacaoRepository;
        this.aulaRepository = aulaRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.participacaoRepository = participacaoRepository;
        this.usuarioAutenticado = usuarioAutenticado;
        this.tagService = tagService;
        this.remocaoArquivos = remocaoArquivos;
        this.mapper = mapper;
    }

    @Override
    @Transactional // anotação e tags novas são salvas juntas: se algo falhar, nada fica salvo pela metade
    public AnotacaoResponseDTO cadastrar(AnotacaoRequestDTO dto) {
        if (dto.aulaId() == null) {
            throw new RequisicaoInvalidaException("O id da aula é obrigatório.");
        }
        String titulo = normalizarTitulo(dto.titulo());
        String conteudo = normalizarConteudo(dto.conteudo());
        exigirTituloOuConteudo(titulo, conteudo);

        Aula aula = aulaRepository.findById(dto.aulaId())
                .orElseThrow(() -> new AulaNaoEncontradaException(dto.aulaId()));

        Anotacao anotacao = new Anotacao();
        anotacao.setTitulo(titulo);
        anotacao.setConteudo(conteudo);
        anotacao.setDataCriacao(LocalDateTime.now());
        anotacao.setAula(aula);
        anotacao.setAutor(usuarioAutenticado.obter());
        tagService.buscarOuCriar(aula.getDisciplina(), dto.tags()).forEach(anotacao::addTag); // tags são opcionais

        Anotacao salva = anotacaoRepository.save(anotacao);
        return mapper.toDto(salva);
    }

    @Override
    @Transactional
    public AnotacaoResponseDTO associarTags(UUID anotacaoId, List<String> nomesTags) {
        if (nomesTags == null || nomesTags.stream().allMatch(nome -> nome == null || nome.isBlank())) {
            throw new RequisicaoInvalidaException("Informe ao menos uma tag.");
        }

        Anotacao anotacao = anotacaoRepository.findById(anotacaoId)
                .orElseThrow(() -> new AnotacaoNaoEncontradaException(anotacaoId));
        // qualquer participante da disciplina pode acrescentar tags, não só o autor (decisão do grupo, #64)
        exigirParticipanteAceito(anotacao.getAula().getDisciplina().getId());

        // as tags são acrescentadas às existentes; como tags é um Set, uma tag já associada não é duplicada
        tagService.buscarOuCriar(anotacao.getAula().getDisciplina(), nomesTags).forEach(anotacao::addTag);

        Anotacao salva = anotacaoRepository.save(anotacao);
        return mapper.toDto(salva);
    }

    @Override
    @Transactional
    public AnotacaoResponseDTO removerTag(UUID anotacaoId, String nomeTag) {
        if (nomeTag == null || nomeTag.isBlank()) {
            throw new RequisicaoInvalidaException("O nome da tag é obrigatório.");
        }

        Anotacao anotacao = anotacaoRepository.findById(anotacaoId)
                .orElseThrow(() -> new AnotacaoNaoEncontradaException(anotacaoId));
        // como acrescentar, remover é permitido a qualquer participante da disciplina
        exigirParticipanteAceito(anotacao.getAula().getDisciplina().getId());

        // a busca ignora maiúsculas, como a criação de tags; uma tag que a anotação não tem não é erro
        String nome = nomeTag.strip();
        anotacao.getTags().stream()
                .filter(tag -> tag.getNome().equalsIgnoreCase(nome))
                .toList()
                .forEach(anotacao::removeTag);

        Anotacao salva = anotacaoRepository.save(anotacao);
        return mapper.toDto(salva);
    }

    @Override
    @Transactional(readOnly = true) // mantém a sessão aberta enquanto o mapper lê as tags (carregadas sob demanda)
    public List<AnotacaoResponseDTO> listarPorAula(UUID aulaId) {
        Aula aula = aulaRepository.findById(aulaId)
                .orElseThrow(() -> new AulaNaoEncontradaException(aulaId));  
        exigirParticipanteAceito(aula.getDisciplina().getId());     //403
        return mapper.toDtoList(anotacaoRepository.findByAulaIdOrderByDataCriacaoAsc(aulaId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnotacaoResponseDTO> listarPorDisciplina(UUID disciplinaId, String tag) {
        if (!disciplinaRepository.existsById(disciplinaId)) {    //404
            throw new DisciplinaNaoEncontradaException(disciplinaId);
        }
        exigirParticipanteAceito(disciplinaId);
        if (tag == null || tag.isBlank()) {
            return mapper.toDtoList(anotacaoRepository.findByAulaDisciplinaIdOrderByAulaDataAscDataCriacaoAsc(disciplinaId));
        }
        return mapper.toDtoList(anotacaoRepository
                .findByAulaDisciplinaIdAndTagsNomeIgnoreCaseOrderByAulaDataAscDataCriacaoAsc(disciplinaId, tag.trim()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnotacaoResponseDTO> listarPorTag(String tag) {
        if (tag == null || tag.isBlank()) {
            throw new RequisicaoInvalidaException("O nome da tag é obrigatório.");
        }
        // sem disciplina informada, a busca vale só para as disciplinas em que o usuário participa (ACEITO)
        List<UUID> disciplinasDoUsuario = participacaoRepository
                .findByUsuarioIdAndStatus(usuarioAutenticado.id(), StatusParticipacao.ACEITO)
                .stream()
                .map(participacao -> participacao.getDisciplina().getId())
                .toList();
        if (disciplinasDoUsuario.isEmpty()) {
            return List.of();
        }
        // tag que não existe não é erro: só não há anotações com ela
        return mapper.toDtoList(anotacaoRepository
                .findByTagsNomeIgnoreCaseAndAulaDisciplinaIdInOrderByAulaDataAscDataCriacaoAsc(tag.trim(), disciplinasDoUsuario));
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

    // acao completa a mensagem de erro, ex.: "editar a anotação" -> "Somente o autor pode editar a anotação."
    private void exigirAutor(Anotacao anotacao, String acao) {
        if (!usuarioAutenticado.id().equals(anotacao.getAutor().getId())) {
            throw new AcessoNegadoException("Somente o autor pode " + acao + ".");
        }
    }

    @Override
    @Transactional
    public AnotacaoResponseDTO editar(UUID id, AnotacaoUpdateDTO dto) {
        String titulo = normalizarTitulo(dto.titulo());
        String conteudo = normalizarConteudo(dto.conteudo());
        exigirTituloOuConteudo(titulo, conteudo);

        Anotacao anotacao = anotacaoRepository.findById(id)
                .orElseThrow(() -> new AnotacaoNaoEncontradaException(id));
        exigirAutor(anotacao, "editar a anotação");

        anotacao.setTitulo(titulo);
        anotacao.setConteudo(conteudo);

        Anotacao salva = anotacaoRepository.save(anotacao);
        return mapper.toDto(salva);
    }

    @Override
    @Transactional
    public void excluir(UUID id) {
        Anotacao anotacao = anotacaoRepository.findById(id)
                .orElseThrow(() -> new AnotacaoNaoEncontradaException(id));
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
    public List<AnotacaoResponseDTO> listarDosColegas(UUID disciplinaId) {
        if (!disciplinaRepository.existsById(disciplinaId)) {    // 404
            throw new DisciplinaNaoEncontradaException(disciplinaId);
    }
    exigirParticipanteAceito(disciplinaId);    // 403
        return mapper.toDtoList(anotacaoRepository
                .findByAulaDisciplinaIdAndAutorIdNotOrderByAulaDataAscDataCriacaoAsc(disciplinaId, usuarioAutenticado.id()));

    }

    // dono e membro têm participação ACEITO; sem participação, pendente ou recusada: sem acesso
    private void exigirParticipanteAceito(UUID disciplinaId) {
        boolean participa = participacaoRepository.existsByUsuarioIdAndDisciplinaIdAndStatus(
                usuarioAutenticado.id(), disciplinaId, StatusParticipacao.ACEITO);
        if (!participa) {
            throw new AcessoNegadoException("Somente participantes da disciplina veem as anotações.");
        }
    }
}