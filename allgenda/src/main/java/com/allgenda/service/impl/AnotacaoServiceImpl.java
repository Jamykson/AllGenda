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

    private final AnotacaoRepository anotacaoRepository;
    private final AulaRepository aulaRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final UsuarioAutenticado usuarioAutenticado;
    private final TagService tagService;
    private final RemocaoArquivos remocaoArquivos;
    private final AnotacaoMapper mapper;

    public AnotacaoServiceImpl(AnotacaoRepository anotacaoRepository,
                                AulaRepository aulaRepository,
                                DisciplinaRepository disciplinaRepository,
                                UsuarioAutenticado usuarioAutenticado,
                                TagService tagService,
                                RemocaoArquivos remocaoArquivos,
                                AnotacaoMapper mapper) {
        this.anotacaoRepository = anotacaoRepository;
        this.aulaRepository = aulaRepository;
        this.disciplinaRepository = disciplinaRepository;
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
        if (dto.conteudo() == null || dto.conteudo().isBlank()) {
            throw new RequisicaoInvalidaException("O conteúdo da anotação é obrigatório.");
        }

        Aula aula = aulaRepository.findById(dto.aulaId())
                .orElseThrow(() -> new AulaNaoEncontradaException(dto.aulaId()));

        Anotacao anotacao = new Anotacao();
        anotacao.setConteudo(dto.conteudo());
        anotacao.setDataCriacao(LocalDateTime.now());
        anotacao.setAula(aula);
        anotacao.setAutor(usuarioAutenticado.obter());
        tagService.buscarOuCriar(dto.tags()).forEach(anotacao::addTag); // tags são opcionais

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
        exigirAutor(anotacao, "alterar as tags da anotação");

        // as tags são acrescentadas às existentes; como tags é um Set, uma tag já associada não é duplicada
        tagService.buscarOuCriar(nomesTags).forEach(anotacao::addTag);

        Anotacao salva = anotacaoRepository.save(anotacao);
        return mapper.toDto(salva);
    }

    @Override
    @Transactional(readOnly = true) // mantém a sessão aberta enquanto o mapper lê as tags (carregadas sob demanda)
    public List<AnotacaoResponseDTO> listarPorAula(UUID aulaId) {
        if (!aulaRepository.existsById(aulaId)) {
            throw new AulaNaoEncontradaException(aulaId);
        }
        return mapper.toDtoList(anotacaoRepository.findByAulaIdOrderByDataCriacaoAsc(aulaId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnotacaoResponseDTO> listarPorDisciplina(UUID disciplinaId, String tag) {
        if (!disciplinaRepository.existsById(disciplinaId)) {
            throw new DisciplinaNaoEncontradaException(disciplinaId);
        }
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
        // tag que não existe não é erro: só não há anotações com ela
        return mapper.toDtoList(anotacaoRepository.findByTagsNomeIgnoreCaseOrderByAulaDataAscDataCriacaoAsc(tag.trim()));
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
        if (dto.conteudo() == null || dto.conteudo().isBlank()) {
            throw new RequisicaoInvalidaException("O conteúdo da anotação é obrigatório.");
        }

        Anotacao anotacao = anotacaoRepository.findById(id)
                .orElseThrow(() -> new AnotacaoNaoEncontradaException(id));
        exigirAutor(anotacao, "editar a anotação");

        anotacao.setConteudo(dto.conteudo());

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
}
