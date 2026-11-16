package com.allgenda.framework.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.allgenda.framework.exception.AcessoNegadoException;
import com.allgenda.framework.exception.ConflitoException;
import com.allgenda.framework.exception.ConviteNaoEncontradoException;
import com.allgenda.framework.exception.GrupoNaoEncontradoException;
import com.allgenda.framework.exception.RequisicaoInvalidaException;
import com.allgenda.framework.exception.UsuarioNaoEncontradoException;
import com.allgenda.framework.model.Grupo;
import com.allgenda.framework.model.Participacao;
import com.allgenda.framework.model.StatusParticipacao;
import com.allgenda.framework.model.Usuario;
import com.allgenda.framework.repository.GrupoRepository;
import com.allgenda.framework.repository.ParticipacaoRepository;
import com.allgenda.framework.repository.UsuarioRepository;
import com.allgenda.framework.service.ConviteService;
import com.allgenda.framework.acesso.AcessoAoGrupo;
import com.allgenda.framework.acesso.Acao;
import com.allgenda.framework.acesso.PoliticaDeAcesso;
import com.allgenda.framework.service.UsuarioAutenticado;

@Service
public class ConviteServiceImpl implements ConviteService {

    //mesmo formato de e-mail aceito no cadastro de usuário.
    private static final Pattern EMAIL = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");

    private final ParticipacaoRepository participacaoRepository;
    private final GrupoRepository grupoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioAutenticado usuarioAutenticado;
    private final AcessoAoGrupo acesso;
    private final PoliticaDeAcesso politica;

    public ConviteServiceImpl(ParticipacaoRepository participacaoRepository,
                              GrupoRepository grupoRepository,
                              UsuarioRepository usuarioRepository,
                              UsuarioAutenticado usuarioAutenticado,
                              AcessoAoGrupo acesso,
                              PoliticaDeAcesso politica) {
        this.participacaoRepository = participacaoRepository;
        this.grupoRepository = grupoRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioAutenticado = usuarioAutenticado;
        this.acesso = acesso;
        this.politica = politica;
    }

    @Override
    @Transactional
    public Participacao enviar(UUID disciplinaId, String emailInformado) {
        String email = normalizarEmail(emailInformado);   // 400

        Grupo disciplina = grupoRepository.findById(disciplinaId)   
          .orElseThrow(() -> new GrupoNaoEncontradoException(disciplinaId)); // 404

    UUID solicitanteId = usuarioAutenticado.id();
    acesso.exigir(Acao.CONVIDAR, disciplinaId, "Você não tem permissão para convidar pessoas para este grupo.");   // 403

    Usuario destinatario = usuarioRepository.findByEmail(email)   // 404
        .orElseThrow(() -> new UsuarioNaoEncontradoException(email));

        if (destinatario.getId().equals(solicitanteId)) {
            throw new ConflitoException("Você não pode convidar a si mesmo."); // 409
        }
        if (participacaoRepository.findByUsuarioIdAndGrupoId(destinatario.getId(), disciplinaId).isPresent()) {
            throw new ConflitoException("Este usuário já tem uma participação nesta disciplina.");   // 409
        }

        // convite = participação com o papel e o status de ingresso definidos pela política; grava-se pelo lado dono (Participacao)
        Participacao convite = new Participacao();
        convite.setUsuario(destinatario);
        convite.setGrupo(disciplina);
        convite.setPapel(politica.papelDoConvidado());
        convite.setStatus(politica.statusAoIngressar());

        return participacaoRepository.save(convite);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Participacao> listar(StatusParticipacao status) {
        UUID usuarioId = usuarioAutenticado.id();
        return 
            participacaoRepository.findByUsuarioIdAndPapelAndStatus(usuarioId, politica.papelDoConvidado(), status);
    }

    @Override
    @Transactional
    public Participacao aceitar(UUID conviteId) {
        return responder(conviteId, StatusParticipacao.ACEITO);
    }

    @Override
    @Transactional
    public Participacao recusar(UUID conviteId) {
        return responder(conviteId, StatusParticipacao.RECUSADO);
    }

    // aceitar e recusar têm as mesmas regras; só muda o status final
    private Participacao responder(UUID conviteId, StatusParticipacao novoStatus) {
        Participacao convite = participacaoRepository.findById(conviteId)       // 404
            .orElseThrow(() -> new ConviteNaoEncontradoException(conviteId));

        if (!convite.getUsuario().getId().equals(usuarioAutenticado.id())) {    // 403
            throw new AcessoNegadoException("Apenas o destinatário pode responder ao convite.");
        }
        if (convite.getStatus() != StatusParticipacao.PENDENTE) {               // 409
            throw new ConflitoException("O convite já foi respondido.");
        }

        convite.setStatus(novoStatus);
        return participacaoRepository.save(convite);
    }


    private String normalizarEmail(String email) {
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new RequisicaoInvalidaException("Informe um e-mail válido.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
