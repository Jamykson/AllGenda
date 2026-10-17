package com.allgenda.service.impl;

import com.allgenda.dto.response.ImagemAnotacaoResponseDTO;
import com.allgenda.exception.AcessoNegadoException;
import com.allgenda.exception.AnotacaoNaoEncontradaException;
import com.allgenda.exception.ArquivoMuitoGrandeException;
import com.allgenda.exception.ImagemNaoEncontradaException;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.mapper.AnotacaoMapper;
import com.allgenda.model.Anotacao;
import com.allgenda.model.ImagemAnotacao;
import com.allgenda.repository.AnotacaoRepository;
import com.allgenda.repository.ImagemAnotacaoRepository;
import com.allgenda.service.ArmazenamentoService;
import com.allgenda.service.ImagemAnotacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class ImagemAnotacaoServiceImpl implements ImagemAnotacaoService {

    private static final Logger log = LoggerFactory.getLogger(ImagemAnotacaoServiceImpl.class);
    private static final String NOME_PADRAO = "imagem";
    private static final int TAMANHO_MAXIMO_NOME = 255;

    private final AnotacaoRepository anotacaoRepository;
    private final ImagemAnotacaoRepository imagemRepository;
    private final ArmazenamentoService armazenamento;
    private final AnotacaoMapper mapper;
    private final DataSize tamanhoMaximo;
    private final int maximoPorAnotacao;

    public ImagemAnotacaoServiceImpl(AnotacaoRepository anotacaoRepository,
                                     ImagemAnotacaoRepository imagemRepository,
                                     ArmazenamentoService armazenamento,
                                     AnotacaoMapper mapper,
                                     @Value("${app.uploads.tamanho-maximo}") DataSize tamanhoMaximo,
                                     @Value("${app.uploads.maximo-por-anotacao}") int maximoPorAnotacao) {
        this.anotacaoRepository = anotacaoRepository;
        this.imagemRepository = imagemRepository;
        this.armazenamento = armazenamento;
        this.mapper = mapper;
        this.tamanhoMaximo = tamanhoMaximo;
        this.maximoPorAnotacao = maximoPorAnotacao;
    }

    @Override
    @Transactional
    public ImagemAnotacaoResponseDTO anexar(UUID anotacaoId, MultipartFile arquivo) {
        Anotacao anotacao = buscarAnotacao(anotacaoId);
        exigirAutor(anotacao);

        if (arquivo == null || arquivo.isEmpty()) {
            throw new RequisicaoInvalidaException("Envie um arquivo de imagem.");
        }
        if (arquivo.getSize() > tamanhoMaximo.toBytes()) {
            throw new ArquivoMuitoGrandeException(tamanhoMaximo);
        }
        if (anotacao.getImagens().size() >= maximoPorAnotacao) {
            throw new RequisicaoInvalidaException("A anotação já tem o máximo de " + maximoPorAnotacao + " imagens.");
        }

        byte[] conteudo = lerConteudo(arquivo);
        // o tipo vem dos primeiros bytes do arquivo; o Content-Type e a extensão enviados pelo cliente não são confiáveis
        TipoImagem tipo = TipoImagem.detectar(conteudo)
                .orElseThrow(() -> new RequisicaoInvalidaException("Formato não suportado. Envie uma imagem PNG, JPEG ou WebP."));

        String chave = armazenamento.salvar(conteudo, tipo.extensao);
        removerArquivoSeNaoConfirmar(chave);

        ImagemAnotacao imagem = new ImagemAnotacao();
        imagem.setNomeOriginal(nomeSeguro(arquivo.getOriginalFilename()));
        imagem.setContentType(tipo.contentType);
        imagem.setTamanho(conteudo.length);
        imagem.setChaveArmazenamento(chave);
        imagem.setDataCriacao(LocalDateTime.now());
        anotacao.addImagem(imagem);

        return mapper.toImagemDto(imagemRepository.save(imagem));
    }

    @Override
    @Transactional(readOnly = true)
    public ImagemArquivo carregar(UUID anotacaoId, UUID imagemId) {
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
        removerArquivoAposConfirmar(imagem.getChaveArmazenamento());
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
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Autenticação obrigatória.");
        }
        UUID usuarioId = UUID.fromString(autenticacao.getName());
        if (!usuarioId.equals(anotacao.getAutor().getId())) {
            throw new AcessoNegadoException("Somente o autor da anotação pode alterar as imagens dela.");
        }
    }

    private static byte[] lerConteudo(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler o arquivo enviado.", e);
        }
    }

    // o nome enviado só é exibido; tira o caminho e caracteres de controle e limita o tamanho
    static String nomeSeguro(String nomeOriginal) {
        if (nomeOriginal == null) {
            return NOME_PADRAO;
        }
        String nome = nomeOriginal.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").strip();
        if (nome.isEmpty()) {
            return NOME_PADRAO;
        }
        return nome.length() > TAMANHO_MAXIMO_NOME ? nome.substring(0, TAMANHO_MAXIMO_NOME) : nome;
    }

    // se a transação falhar depois de o arquivo ser gravado, o arquivo é apagado para não ficar órfão no disco
    private void removerArquivoSeNaoConfirmar(String chave) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    removerArquivo(chave);
                }
            }
        });
    }

    // o arquivo só é apagado depois que a remoção do registro foi confirmada no banco
    private void removerArquivoAposConfirmar(String chave) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            removerArquivo(chave);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                removerArquivo(chave);
            }
        });
    }

    // falhar ao apagar o arquivo não deve desfazer nem quebrar uma operação já confirmada no banco
    private void removerArquivo(String chave) {
        try {
            armazenamento.remover(chave);
        } catch (RuntimeException e) {
            log.warn("Não foi possível remover o arquivo {} do armazenamento.", chave, e);
        }
    }

    private enum TipoImagem {
        PNG("image/png", "png"),
        JPEG("image/jpeg", "jpg"),
        WEBP("image/webp", "webp");

        private final String contentType;
        private final String extensao;

        TipoImagem(String contentType, String extensao) {
            this.contentType = contentType;
            this.extensao = extensao;
        }

        static Optional<TipoImagem> detectar(byte[] conteudo) {
            if (comecaCom(conteudo, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
                return Optional.of(PNG);
            }
            if (comecaCom(conteudo, 0, 0xFF, 0xD8, 0xFF)) {
                return Optional.of(JPEG);
            }
            if (comecaCom(conteudo, 0, 'R', 'I', 'F', 'F') && comecaCom(conteudo, 8, 'W', 'E', 'B', 'P')) {
                return Optional.of(WEBP);
            }
            return Optional.empty();
        }

        private static boolean comecaCom(byte[] conteudo, int inicio, int... esperados) {
            if (conteudo.length < inicio + esperados.length) {
                return false;
            }
            for (int i = 0; i < esperados.length; i++) {
                if ((conteudo[inicio + i] & 0xFF) != esperados[i]) {
                    return false;
                }
            }
            return true;
        }
    }
}
