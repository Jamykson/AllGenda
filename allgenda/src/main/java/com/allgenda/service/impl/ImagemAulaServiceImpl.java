package com.allgenda.service.impl;

import com.allgenda.dto.request.ImagemAulaUpdateDTO;
import com.allgenda.dto.response.ImagemAulaResponseDTO;
import com.allgenda.exception.AcessoNegadoException;
import com.allgenda.exception.AulaNaoEncontradaException;
import com.allgenda.exception.ImagemNaoEncontradaException;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.mapper.ImagemAulaMapper;
import com.allgenda.model.Aula;
import com.allgenda.model.ImagemAula;
import com.allgenda.repository.AulaRepository;
import com.allgenda.repository.ImagemAulaRepository;
import com.allgenda.service.ArmazenamentoService;
import com.allgenda.service.ImagemArquivo;
import com.allgenda.service.ImagemAulaService;
import com.allgenda.service.RecebimentoImagem;
import com.allgenda.service.RecebimentoImagem.ImagemRecebida;
import com.allgenda.service.RemocaoArquivos;
import com.allgenda.service.AcessoDisciplina;
import com.allgenda.service.UsuarioAutenticado;
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
public class ImagemAulaServiceImpl implements ImagemAulaService {

    private static final int TAMANHO_MAXIMO_LEGENDA = 300;

    private final AulaRepository aulaRepository;
    private final ImagemAulaRepository imagemRepository;
    private final AcessoDisciplina acesso;
    private final RecebimentoImagem recebimento;
    private final ArmazenamentoService armazenamento;
    private final RemocaoArquivos remocaoArquivos;
    private final ImagemAulaMapper mapper;
    private final UsuarioAutenticado usuarioAutenticado;

    public ImagemAulaServiceImpl(AulaRepository aulaRepository,
                                 ImagemAulaRepository imagemRepository,
                                 AcessoDisciplina acesso,
                                 RecebimentoImagem recebimento,
                                 ArmazenamentoService armazenamento,
                                 RemocaoArquivos remocaoArquivos,
                                 ImagemAulaMapper mapper,
                                 UsuarioAutenticado usuarioAutenticado) {
        this.aulaRepository = aulaRepository;
        this.imagemRepository = imagemRepository;
        this.acesso = acesso;
        this.recebimento = recebimento;
        this.armazenamento = armazenamento;
        this.remocaoArquivos = remocaoArquivos;
        this.mapper = mapper;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    @Override
    @Transactional
    public ImagemAulaResponseDTO adicionar(UUID aulaId, MultipartFile arquivo, String legenda) {
        Aula aula = buscarAula(aulaId);
        exigirParticipanteAceito(aula);
        String legendaNormalizada = normalizarLegenda(legenda); // antes de gravar o arquivo

        ImagemRecebida recebida = recebimento.receber(arquivo);

        ImagemAula imagem = new ImagemAula();
        imagem.setLegenda(legendaNormalizada);
        imagem.setNomeOriginal(recebida.nomeOriginal());
        imagem.setContentType(recebida.contentType());
        imagem.setTamanho(recebida.tamanho());
        imagem.setChaveArmazenamento(recebida.chave());
        imagem.setDataCriacao(LocalDateTime.now());
        imagem.setPosicao(imagemRepository.findMaiorPosicaoByAulaId(aulaId) + 1); // entra no fim da galeria
        imagem.setAutor(usuarioAutenticado.obter());
        aula.addImagem(imagem);

        return mapper.toDto(imagemRepository.save(imagem));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImagemAulaResponseDTO> listarPorAula(UUID aulaId) {
        exigirParticipanteAceito(buscarAula(aulaId));
        return mapper.toDtoList(imagemRepository.findByAulaIdOrderByPosicaoAscDataCriacaoAsc(aulaId));
    }

    @Override
    @Transactional
    public List<ImagemAulaResponseDTO> reordenar(UUID aulaId, List<UUID> ids) {
        exigirParticipanteAceito(buscarAula(aulaId));
        List<ImagemAula> imagens = imagemRepository.findByAulaIdOrderByPosicaoAscDataCriacaoAsc(aulaId);

        // a lista precisa ter cada imagem da aula exatamente uma vez; outra combinação provavelmente
        // vem de uma tela desatualizada, e aplicá-la deixaria a ordem inconsistente
        Map<UUID, ImagemAula> porId = imagens.stream().collect(Collectors.toMap(ImagemAula::getId, imagem -> imagem));
        if (ids == null || ids.size() != imagens.size() || !porId.keySet().equals(new HashSet<>(ids))) {
            throw new RequisicaoInvalidaException("A nova ordem deve listar exatamente as imagens da aula.");
        }

        for (int i = 0; i < ids.size(); i++) {
            porId.get(ids.get(i)).setPosicao(i);
        }
        return mapper.toDtoList(imagemRepository.findByAulaIdOrderByPosicaoAscDataCriacaoAsc(aulaId));
    }

    @Override
    @Transactional(readOnly = true)
    public ImagemArquivo carregar(UUID aulaId, UUID imagemId) {
        exigirParticipanteAceito(buscarAula(aulaId));
        ImagemAula imagem = buscarImagem(aulaId, imagemId);
        return armazenamento.carregar(imagem.getChaveArmazenamento())
                .map(conteudo -> new ImagemArquivo(conteudo, imagem.getContentType(), imagem.getNomeOriginal()))
                .orElseThrow(() -> new ImagemNaoEncontradaException(imagemId));
    }

    @Override
    @Transactional
    public ImagemAulaResponseDTO editarLegenda(UUID aulaId, UUID imagemId, ImagemAulaUpdateDTO dto) {
        String legenda = normalizarLegenda(dto.legenda());
        ImagemAula imagem = buscarImagem(aulaId, imagemId);
        exigirAutor(imagem);

        imagem.setLegenda(legenda);
        return mapper.toDto(imagemRepository.save(imagem));
    }

    @Override
    @Transactional
    public void remover(UUID aulaId, UUID imagemId) {
        ImagemAula imagem = buscarImagem(aulaId, imagemId);
        exigirAutor(imagem);

        imagem.getAula().removeImagem(imagem);
        remocaoArquivos.removerAoConfirmar(List.of(imagem.getChaveArmazenamento()));
    }

    private Aula buscarAula(UUID aulaId) {
        return aulaRepository.findById(aulaId)
                .orElseThrow(() -> new AulaNaoEncontradaException(aulaId));
    }

    private ImagemAula buscarImagem(UUID aulaId, UUID imagemId) {
        return imagemRepository.findByIdAndAulaId(imagemId, aulaId)
                .orElseThrow(() -> new ImagemNaoEncontradaException(imagemId));
    }

    private void exigirAutor(ImagemAula imagem) {
        if (!usuarioAutenticado.id().equals(imagem.getAutor().getId())) {
            throw new AcessoNegadoException("Somente o autor da imagem pode alterá-la.");
        }
    }

    private void exigirParticipanteAceito(Aula aula) {
        acesso.exigirParticipanteAceito(aula.getDisciplina().getId(), "Somente participantes da disciplina veem as imagens.");
    }

    // legenda em branco vira null; o limite acompanha a coluna legenda de ImagemAula
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
