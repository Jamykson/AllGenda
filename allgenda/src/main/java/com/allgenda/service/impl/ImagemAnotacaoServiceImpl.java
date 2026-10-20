package com.allgenda.service.impl;

import com.allgenda.dto.response.ImagemAnotacaoResponseDTO;
import com.allgenda.exception.AcessoNegadoException;
import com.allgenda.exception.AnotacaoNaoEncontradaException;
import com.allgenda.exception.ImagemNaoEncontradaException;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.mapper.AnotacaoMapper;
import com.allgenda.model.Anotacao;
import com.allgenda.model.ImagemAnotacao;
import com.allgenda.repository.AnotacaoRepository;
import com.allgenda.repository.ImagemAnotacaoRepository;
import com.allgenda.service.AcessoDisciplina;
import com.allgenda.service.ArmazenamentoService;
import com.allgenda.service.ImagemAnotacaoService;
import com.allgenda.service.ImagemArquivo;
import com.allgenda.service.RecebimentoImagem;
import com.allgenda.service.RecebimentoImagem.ImagemRecebida;
import com.allgenda.service.RemocaoArquivos;
import com.allgenda.service.UsuarioAutenticado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ImagemAnotacaoServiceImpl implements ImagemAnotacaoService {

    private final AnotacaoRepository anotacaoRepository;
    private final ImagemAnotacaoRepository imagemRepository;
    private final RecebimentoImagem recebimento;
    private final ArmazenamentoService armazenamento;
    private final RemocaoArquivos remocaoArquivos;
    private final AnotacaoMapper mapper;
    private final UsuarioAutenticado usuarioAutenticado;
    private final AcessoDisciplina acesso;
    private final int maximoPorAnotacao;

    public ImagemAnotacaoServiceImpl(AnotacaoRepository anotacaoRepository,
                                     ImagemAnotacaoRepository imagemRepository,
                                     RecebimentoImagem recebimento,
                                     ArmazenamentoService armazenamento,
                                     RemocaoArquivos remocaoArquivos,
                                     AnotacaoMapper mapper,
                                     UsuarioAutenticado usuarioAutenticado,
                                     AcessoDisciplina acesso,
                                     @Value("${app.uploads.maximo-por-anotacao}") int maximoPorAnotacao) {
        this.anotacaoRepository = anotacaoRepository;
        this.imagemRepository = imagemRepository;
        this.recebimento = recebimento;
        this.armazenamento = armazenamento;
        this.remocaoArquivos = remocaoArquivos;
        this.mapper = mapper;
        this.usuarioAutenticado = usuarioAutenticado;
        this.acesso = acesso;
        this.maximoPorAnotacao = maximoPorAnotacao;
    }

    @Override
    @Transactional
    public ImagemAnotacaoResponseDTO anexar(UUID anotacaoId, MultipartFile arquivo) {
        Anotacao anotacao = buscarAnotacao(anotacaoId);
        exigirAutor(anotacao);

        recebimento.validarEnvio(arquivo);
        if (anotacao.getImagens().size() >= maximoPorAnotacao) {
            throw new RequisicaoInvalidaException("A anotação já tem o máximo de " + maximoPorAnotacao + " imagens.");
        }

        ImagemRecebida recebida = recebimento.receber(arquivo);

        ImagemAnotacao imagem = new ImagemAnotacao();
        imagem.setNomeOriginal(recebida.nomeOriginal());
        imagem.setContentType(recebida.contentType());
        imagem.setTamanho(recebida.tamanho());
        imagem.setChaveArmazenamento(recebida.chave());
        imagem.setDataCriacao(LocalDateTime.now());
        anotacao.addImagem(imagem);

        return mapper.toImagemDto(imagemRepository.save(imagem));
    }

    @Override
    @Transactional(readOnly = true)
    public ImagemArquivo carregar(UUID anotacaoId, UUID imagemId) {
        Anotacao anotacao = buscarAnotacao(anotacaoId);                       // 404
        acesso.exigirParticipanteAceito(anotacao.getAula().getDisciplina().getId(),
                "Somente participantes da disciplina veem as imagens das anotações."); // 403
        ImagemAnotacao imagem = buscarImagem(anotacaoId, imagemId);
        return armazenamento.carregar(imagem.getChaveArmazenamento())
                .map(conteudo -> new ImagemArquivo(conteudo, imagem.getContentType(), imagem.getNomeOriginal()))
                .orElseThrow(() -> new ImagemNaoEncontradaException(imagemId));
    }

    @Override
    @Transactional
    public void remover(UUID anotacaoId, UUID imagemId) {
        Anotacao anotacao = buscarAnotacao(anotacaoId);
        exigirAutor(anotacao); // antes de procurar a imagem, para quem não é autor não descobrir quais ids existem
        ImagemAnotacao imagem = buscarImagem(anotacaoId, imagemId);

        anotacao.removeImagem(imagem);
        remocaoArquivos.removerAoConfirmar(List.of(imagem.getChaveArmazenamento()));
    }

    private Anotacao buscarAnotacao(UUID anotacaoId) {
        return anotacaoRepository.findById(anotacaoId)
                .orElseThrow(() -> new AnotacaoNaoEncontradaException(anotacaoId));
    }

    private ImagemAnotacao buscarImagem(UUID anotacaoId, UUID imagemId) {
        return imagemRepository.findByIdAndAnotacaoId(imagemId, anotacaoId)
                .orElseThrow(() -> new ImagemNaoEncontradaException(imagemId));
    }

    private void exigirAutor(Anotacao anotacao) {
        if (!usuarioAutenticado.id().equals(anotacao.getAutor().getId())) {
            throw new AcessoNegadoException("Somente o autor da anotação pode alterar as imagens dela.");
        }
    }
}
