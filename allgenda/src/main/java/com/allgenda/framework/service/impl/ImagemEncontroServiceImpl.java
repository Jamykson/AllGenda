package com.allgenda.framework.service.impl;

import com.allgenda.framework.exception.AcessoNegadoException;
import com.allgenda.framework.exception.EncontroNaoEncontradoException;
import com.allgenda.framework.exception.ImagemNaoEncontradaException;
import com.allgenda.framework.exception.RequisicaoInvalidaException;
import com.allgenda.framework.model.Encontro;
import com.allgenda.framework.model.ImagemEncontro;
import com.allgenda.framework.repository.EncontroRepository;
import com.allgenda.framework.repository.ImagemEncontroRepository;
import com.allgenda.framework.service.ArmazenamentoService;
import com.allgenda.framework.service.ImagemArquivo;
import com.allgenda.framework.service.ImagemEncontroService;
import com.allgenda.framework.service.RecebimentoImagem;
import com.allgenda.framework.service.RecebimentoImagem.ImagemRecebida;
import com.allgenda.framework.service.RemocaoArquivos;
import com.allgenda.framework.acesso.AcessoAoGrupo;
import com.allgenda.framework.acesso.Acao;
import com.allgenda.framework.service.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ImagemEncontroServiceImpl implements ImagemEncontroService {

    private static final int TAMANHO_MAXIMO_LEGENDA = 300;

    private final EncontroRepository encontroRepository;
    private final ImagemEncontroRepository imagemRepository;
    private final AcessoAoGrupo acesso;
    private final RecebimentoImagem recebimento;
    private final ArmazenamentoService armazenamento;
    private final RemocaoArquivos remocaoArquivos;
    private final UsuarioAutenticado usuarioAutenticado;

    public ImagemEncontroServiceImpl(EncontroRepository encontroRepository,
                                 ImagemEncontroRepository imagemRepository,
                                 AcessoAoGrupo acesso,
                                 RecebimentoImagem recebimento,
                                 ArmazenamentoService armazenamento,
                                 RemocaoArquivos remocaoArquivos,
                                 UsuarioAutenticado usuarioAutenticado) {
        this.encontroRepository = encontroRepository;
        this.imagemRepository = imagemRepository;
        this.acesso = acesso;
        this.recebimento = recebimento;
        this.armazenamento = armazenamento;
        this.remocaoArquivos = remocaoArquivos;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    @Override
    @Transactional
    public ImagemEncontro adicionar(UUID encontroId, MultipartFile arquivo, String legenda) {
        Encontro encontro = buscarEncontro(encontroId);
        exigir(Acao.CRIAR_ANOTACAO, encontro);
        String legendaNormalizada = normalizarLegenda(legenda); // antes de gravar o arquivo

        ImagemRecebida recebida = recebimento.receber(arquivo);

        ImagemEncontro imagem = new ImagemEncontro();
        imagem.setLegenda(legendaNormalizada);
        imagem.setNomeOriginal(recebida.nomeOriginal());
        imagem.setContentType(recebida.contentType());
        imagem.setTamanho(recebida.tamanho());
        imagem.setChaveArmazenamento(recebida.chave());
        imagem.setDataCriacao(LocalDateTime.now());
        imagem.setPosicao(imagemRepository.findMaiorPosicaoByEncontroId(encontroId) + 1); // entra no fim da galeria
        imagem.setAutor(usuarioAutenticado.obter());
        encontro.addImagem(imagem);

        return imagemRepository.save(imagem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImagemEncontro> listarPorEncontro(UUID encontroId) {
        exigir(Acao.VER_CONTEUDO, buscarEncontro(encontroId));
        return imagemRepository.findByEncontroIdOrderByPosicaoAscDataCriacaoAsc(encontroId);
    }

    @Override
    @Transactional
    public List<ImagemEncontro> reordenar(UUID encontroId, List<UUID> ids) {
        exigir(Acao.CRIAR_ANOTACAO, buscarEncontro(encontroId)); // reordenar a galeria faz parte de anotar
        List<ImagemEncontro> imagens = imagemRepository.findByEncontroIdOrderByPosicaoAscDataCriacaoAsc(encontroId);

        // a lista precisa ter cada imagem do encontro exatamente uma vez; outra combinação provavelmente
        // vem de uma tela desatualizada, e aplicá-la deixaria a ordem inconsistente
        Map<UUID, ImagemEncontro> porId = imagens.stream().collect(Collectors.toMap(ImagemEncontro::getId, imagem -> imagem));
        if (ids == null || ids.size() != imagens.size() || !porId.keySet().equals(new HashSet<>(ids))) {
            throw new RequisicaoInvalidaException("A nova ordem deve listar exatamente as imagens do encontro.");
        }

        for (int i = 0; i < ids.size(); i++) {
            porId.get(ids.get(i)).setPosicao(i);
        }
        return imagemRepository.findByEncontroIdOrderByPosicaoAscDataCriacaoAsc(encontroId);
    }

    @Override
    @Transactional(readOnly = true)
    public ImagemArquivo carregar(UUID encontroId, UUID imagemId) {
        exigir(Acao.VER_CONTEUDO, buscarEncontro(encontroId));
        ImagemEncontro imagem = buscarImagem(encontroId, imagemId);
        return armazenamento.carregar(imagem.getChaveArmazenamento())
                .map(conteudo -> new ImagemArquivo(conteudo, imagem.getContentType(), imagem.getNomeOriginal()))
                .orElseThrow(() -> new ImagemNaoEncontradaException(imagemId));
    }

    @Override
    @Transactional
    public ImagemEncontro editarLegenda(UUID encontroId, UUID imagemId, String legendaInformada) {
        String legenda = normalizarLegenda(legendaInformada);
        ImagemEncontro imagem = buscarImagem(encontroId, imagemId);
        exigirAutor(imagem);

        imagem.setLegenda(legenda);
        return imagemRepository.save(imagem);
    }

    @Override
    @Transactional
    public void remover(UUID encontroId, UUID imagemId) {
        ImagemEncontro imagem = buscarImagem(encontroId, imagemId);
        exigirAutor(imagem);

        imagem.getEncontro().removeImagem(imagem);
        remocaoArquivos.removerAoConfirmar(List.of(imagem.getChaveArmazenamento()));
    }

    private Encontro buscarEncontro(UUID encontroId) {
        return encontroRepository.findById(encontroId)
                .orElseThrow(() -> new EncontroNaoEncontradoException(encontroId));
    }

    private ImagemEncontro buscarImagem(UUID encontroId, UUID imagemId) {
        return imagemRepository.findByIdAndEncontroId(imagemId, encontroId)
                .orElseThrow(() -> new ImagemNaoEncontradaException(imagemId));
    }

    private void exigirAutor(ImagemEncontro imagem) {
        if (!usuarioAutenticado.id().equals(imagem.getAutor().getId())) {
            throw new AcessoNegadoException("Somente o autor da imagem pode alterá-la.");
        }
    }

    private void exigir(Acao acao, Encontro encontro) {
        acesso.exigir(acao, encontro.getGrupo().getId(), "Somente participantes do grupo acessam as imagens dos encontros.");
    }

    // legenda em branco vira null; o limite acompanha a coluna legenda de ImagemEncontro
    private static String normalizarLegenda(String legenda) {
        if (legenda == null || legenda.isBlank()) {
            return null;
        }
        String semEspacos = legenda.strip();
        if (semEspacos.length() > TAMANHO_MAXIMO_LEGENDA) {
            throw new RequisicaoInvalidaException("A legenda deve ter no máximo " + TAMANHO_MAXIMO_LEGENDA + " caracteres.");
        }
        return semEspacos;
    }
}
