package com.allgenda.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.allgenda.dto.request.ConviteRequestDTO;
import com.allgenda.dto.response.ConviteResponseDTO;
import com.allgenda.exception.AcessoNegadoException;
import com.allgenda.exception.ConflitoException;
import com.allgenda.exception.ConviteNaoEncontradoException;
import com.allgenda.exception.DisciplinaNaoEncontradaException;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.exception.UsuarioNaoEncontradoException;
import com.allgenda.mapper.ConviteMapper;
import com.allgenda.model.Disciplina;
import com.allgenda.model.PapelParticipacao;
import com.allgenda.model.Participacao;
import com.allgenda.model.StatusParticipacao;
import com.allgenda.model.Usuario;
import com.allgenda.repository.DisciplinaRepository;
import com.allgenda.repository.ParticipacaoRepository;
import com.allgenda.repository.UsuarioRepository;
import com.allgenda.service.ConviteService;
import com.allgenda.service.UsuarioAutenticado;

@Service
public class ConviteServiceImpl implements ConviteService {

    //mesmo formato de e-mail aceito no cadastro de usuário.
    private static final Pattern EMAIL = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");

    private final ParticipacaoRepository participacaoRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioAutenticado usuarioAutenticado;
    private final ConviteMapper mapper;

    public ConviteServiceImpl(ParticipacaoRepository participacaoRepository,
                              DisciplinaRepository disciplinaRepository,
                              UsuarioRepository usuarioRepository,
                              UsuarioAutenticado usuarioAutenticado,
                              ConviteMapper mapper) {
        this.participacaoRepository = participacaoRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioAutenticado = usuarioAutenticado;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ConviteResponseDTO enviar(UUID disciplinaId, ConviteRequestDTO dto) {
        String email = normalizarEmail(dto.email());   // 400

        Disciplina disciplina = disciplinaRepository.findById(disciplinaId)   // 404
          .orElseThrow(() -> new DisciplinaNaoEncontradaException(disciplinaId));

    UUID solicitanteId = usuarioAutenticado.id();
    exigirDonoAceito(solicitanteId, disciplinaId);   // 403

    Usuario destinatario = usuarioRepository.findByEmail(email)   // 404
        .orElseThrow(() -> new UsuarioNaoEncontradoException(email));

        if (destinatario.getId().equals(solicitanteId)) {                       // 409
            throw new ConflitoException("Você não pode convidar a si mesmo.");
        }
        if (participacaoRepository.findByUsuarioIdAndDisciplinaId(destinatario.getId(), disciplinaId).isPresent()) {
            throw new ConflitoException("Este usuário já tem uma participação nesta disciplina.");
        }

        // convite = participação de membro ainda pendente; grava-se pelo lado dono (Participacao)
        Participacao convite = new Participacao();
        convite.setUsuario(destinatario);
        convite.setDisciplina(disciplina);
        convite.setPapel(PapelParticipacao.MEMBRO);
        convite.setStatus(StatusParticipacao.PENDENTE);

        return mapper.toDto(participacaoRepository.save(convite));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConviteResponseDTO> listar(StatusParticipacao status) {
        UUID usuarioId = usuarioAutenticado.id();
        return mapper.toDtoList(
            participacaoRepository.findByUsuarioIdAndPapelAndStatus(usuarioId, PapelParticipacao.MEMBRO, status));
    }

    @Override
    @Transactional
    public ConviteResponseDTO aceitar(UUID conviteId) {
        return responder(conviteId, StatusParticipacao.ACEITO);
    }

    @Override
    @Transactional
    public ConviteResponseDTO recusar(UUID conviteId) {
        return responder(conviteId, StatusParticipacao.RECUSADO);
    }

    // aceitar e recusar têm as mesmas regras; só muda o status final
    private ConviteResponseDTO responder(UUID conviteId, StatusParticipacao novoStatus) {
        Participacao convite = participacaoRepository.findById(conviteId)       // 404
            .orElseThrow(() -> new ConviteNaoEncontradoException(conviteId));

        if (!convite.getUsuario().getId().equals(usuarioAutenticado.id())) {    // 403
            throw new AcessoNegadoException("Apenas o destinatário pode responder ao convite.");
        }
        if (convite.getStatus() != StatusParticipacao.PENDENTE) {               // 409
            throw new ConflitoException("O convite já foi respondido.");
        }

        convite.setStatus(novoStatus);
        return mapper.toDto(participacaoRepository.save(convite));
    }

    // regra do contrato: só o dono (com participação aceita) envia convites
    private void exigirDonoAceito(UUID usuarioId, UUID disciplinaId) {
        boolean donoAceito = participacaoRepository.findByUsuarioIdAndDisciplinaId(usuarioId, disciplinaId)
            .filter(p -> p.getPapel() == PapelParticipacao.DONO && p.getStatus() == StatusParticipacao.ACEITO)
            .isPresent();
        if (!donoAceito) {
            throw new AcessoNegadoException("Apenas o dono pode enviar convites.");
        }
    }

    private String normalizarEmail(String email) {
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new RequisicaoInvalidaException("Informe um e-mail válido.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
