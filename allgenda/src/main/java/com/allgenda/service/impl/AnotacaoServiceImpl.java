package com.allgenda.service.impl;

import com.allgenda.config.UsuarioPadraoInitializer;
import com.allgenda.dto.request.AnotacaoRequestDTO;
import com.allgenda.dto.response.AnotacaoResponseDTO;
import com.allgenda.exception.AnotacaoNaoEncontradaException;
import com.allgenda.exception.AulaNaoEncontradaException;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.exception.UsuarioNaoEncontradoException;
import com.allgenda.mapper.AnotacaoMapper;
import com.allgenda.model.Anotacao;
import com.allgenda.model.Aula;
import com.allgenda.model.Usuario;
import com.allgenda.repository.AnotacaoRepository;
import com.allgenda.repository.AulaRepository;
import com.allgenda.repository.UsuarioRepository;
import com.allgenda.service.AnotacaoService;
import com.allgenda.service.TagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AnotacaoServiceImpl implements AnotacaoService {

    private final AnotacaoRepository anotacaoRepository;
    private final AulaRepository aulaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TagService tagService;
    private final AnotacaoMapper mapper;

    public AnotacaoServiceImpl(AnotacaoRepository anotacaoRepository,
                                AulaRepository aulaRepository,
                                UsuarioRepository usuarioRepository,
                                TagService tagService,
                                AnotacaoMapper mapper) {
        this.anotacaoRepository = anotacaoRepository;
        this.aulaRepository = aulaRepository;
        this.usuarioRepository = usuarioRepository;
        this.tagService = tagService;
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
        anotacao.setAutor(buscarAutor(dto.autorId()));
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

        // as tags são acrescentadas às existentes; como tags é um Set, uma tag já associada não é duplicada
        tagService.buscarOuCriar(nomesTags).forEach(anotacao::addTag);

        Anotacao salva = anotacaoRepository.save(anotacao);
        return mapper.toDto(salva);
    }

    // autorId é temporário até a sprint 3; sem ele, a anotação fica no nome do usuário padrão
    private Usuario buscarAutor(UUID autorId) {
        if (autorId != null) {
            return usuarioRepository.findById(autorId)
                    .orElseThrow(() -> new UsuarioNaoEncontradoException(autorId));
        }
        return usuarioRepository.findByNome(UsuarioPadraoInitializer.NOME_USUARIO_PADRAO)
                .orElseThrow(() -> new IllegalStateException("Usuário padrão não foi criado na inicialização."));
    }
}
