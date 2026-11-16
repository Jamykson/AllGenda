package com.allgenda.service.impl;

import com.allgenda.exception.GrupoNaoEncontradoException;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.framework.acesso.AcessoAoGrupo;
import com.allgenda.framework.acesso.Acao;
import com.allgenda.framework.extensao.DadosDoEvento;
import com.allgenda.framework.extensao.FabricaDeEvento;
import com.allgenda.framework.extensao.RegistroDeTipos;
import com.allgenda.model.Evento;
import com.allgenda.model.Grupo;
import com.allgenda.repository.EventoRepository;
import com.allgenda.repository.GrupoRepository;
import com.allgenda.service.EventoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EventoServiceImpl implements EventoService {

    private static final String SEM_ACESSO = "Somente participantes do grupo acessam os eventos dele.";
    private static final int TAMANHO_MAXIMO_DESCRICAO = 500;

    private final EventoRepository eventos;
    private final GrupoRepository grupos;
    private final AcessoAoGrupo acesso;
    private final RegistroDeTipos registro;

    public EventoServiceImpl(EventoRepository eventos, GrupoRepository grupos, AcessoAoGrupo acesso,
                             RegistroDeTipos registro) {
        this.eventos = eventos;
        this.grupos = grupos;
        this.acesso = acesso;
        this.registro = registro;
    }

    // Fluxo fixo: valida o que é comum, confere o grupo e a permissão e pede à fábrica do tipo a subclasse
    // da aplicação, que valida o que é dela (o subtipo). Depois preenche os campos comuns.
    @Override
    @Transactional
    public Evento criar(String tipo, DadosDoEvento dados) {
        if (dados.grupoId() == null) {
            throw new RequisicaoInvalidaException("O id do grupo é obrigatório.");
        }
        if (dados.descricao() == null || dados.descricao().isBlank() || dados.descricao().trim().length() > TAMANHO_MAXIMO_DESCRICAO) {
            throw new RequisicaoInvalidaException("Informe uma descrição de até " + TAMANHO_MAXIMO_DESCRICAO + " caracteres.");
        }
        if (dados.data() == null) {
            throw new RequisicaoInvalidaException("A data do evento é obrigatória.");
        }
        FabricaDeEvento fabrica = registro.fabricaDeEvento(tipo);       // 400 se o tipo não existir

        Grupo grupo = grupos.findById(dados.grupoId())
                .orElseThrow(() -> new GrupoNaoEncontradoException(dados.grupoId()));  // 404
        acesso.exigir(Acao.CRIAR_EVENTO, grupo.getId(), SEM_ACESSO);                   // 403

        Evento evento = fabrica.criar(dados, grupo);                                    // 400 se o subtipo for inválido
        evento.setGrupo(grupo);
        evento.setDescricao(dados.descricao().trim());
        evento.setData(dados.data());
        return eventos.save(evento);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Evento> listarPorGrupo(UUID grupoId) {
        if (grupoId == null) {
            throw new RequisicaoInvalidaException("O id do grupo é obrigatório.");
        }
        // diferencia um grupo sem eventos de um grupo inexistente
        if (!grupos.existsById(grupoId)) {
            throw new GrupoNaoEncontradoException(grupoId);
        }
        acesso.exigir(Acao.VER_CONTEUDO, grupoId, SEM_ACESSO);
        return eventos.findByGrupoIdOrderByDataAscIdAsc(grupoId);
    }
}
