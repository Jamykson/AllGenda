package com.allgenda.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.allgenda.exception.GrupoNaoEncontradoException;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.model.Disciplina;
import com.allgenda.model.Grupo;
import com.allgenda.model.Participacao;
import com.allgenda.model.StatusParticipacao;
import com.allgenda.repository.GrupoRepository;
import com.allgenda.repository.ParticipacaoRepository;
import com.allgenda.repository.RecorrenciaRepository;
import com.allgenda.repository.ImagemAnotacaoRepository;
import com.allgenda.repository.ImagemEncontroRepository;
import com.allgenda.framework.acesso.AcessoAoGrupo;
import com.allgenda.framework.acesso.Acao;
import com.allgenda.framework.acesso.PoliticaDeAcesso;
import com.allgenda.service.DadosDoGrupo;
import com.allgenda.service.DisciplinaService;
import com.allgenda.service.RemocaoArquivos;
import com.allgenda.service.UsuarioAutenticado;

@Service
public class DisciplinaServiceImpl implements DisciplinaService {

    private final GrupoRepository repository;
    private final ParticipacaoRepository participacaoRepository;
    private final RecorrenciaRepository recorrenciaRepository;
    private final ImagemAnotacaoRepository imagemRepository;
    private final ImagemEncontroRepository imagemEncontroRepository;
    private final RemocaoArquivos remocaoArquivos;
    private final UsuarioAutenticado usuarioAutenticado;
    private final AcessoAoGrupo acesso;
    private final PoliticaDeAcesso politica;

    public DisciplinaServiceImpl(GrupoRepository repository,
                                 ParticipacaoRepository participacaoRepository,
                                 RecorrenciaRepository recorrenciaRepository,
                                 ImagemAnotacaoRepository imagemRepository,
                                 ImagemEncontroRepository imagemEncontroRepository,
                                 RemocaoArquivos remocaoArquivos,
                                 UsuarioAutenticado usuarioAutenticado,
                                 AcessoAoGrupo acesso,
                                 PoliticaDeAcesso politica) {
        this.repository = repository;
        this.participacaoRepository = participacaoRepository;
        this.recorrenciaRepository = recorrenciaRepository;
        this.imagemRepository = imagemRepository;
        this.imagemEncontroRepository = imagemEncontroRepository;
        this.remocaoArquivos = remocaoArquivos;
        this.usuarioAutenticado = usuarioAutenticado;
        this.acesso = acesso;
        this.politica = politica;
    }

    @Override
    @Transactional
    public Grupo cadastrar(DadosDoGrupo dto) {
        if(dto.nome() == null || dto.nome().isBlank()){
            throw new RequisicaoInvalidaException("O nome da disciplina é obrigatório.");
        }

        String nome = dto.nome().trim();
        String descricao = dto.descricao() == null ? null : dto.descricao().trim();

        if (descricao != null && descricao.length() > 500) {
            throw new RequisicaoInvalidaException("A descrição deve ter no máximo 500 caracteres.");
        }

        Grupo disciplina = new Disciplina();
        disciplina.setNome(nome);
        disciplina.setDescricao(descricao == null || descricao.isBlank() ? null : descricao);

        // quem cria o grupo recebe o papel de criador definido pela política, com a participação já aceita
        Participacao participacao = new Participacao();
        participacao.setUsuario(usuarioAutenticado.obter());
        participacao.setPapel(politica.papelDoCriador());
        participacao.setStatus(StatusParticipacao.ACEITO);
        Grupo salva = repository.save(disciplina);
        salva.addParticipacao(participacao);
        // A associação não usa cascata: disciplina e dono são gravados na mesma transação.
        participacaoRepository.save(participacao);
        return salva;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Grupo> listarDoUsuario() {
        // só as disciplinas em que a pessoa participa com status ACEITO (dono ou membro)
        return participacaoRepository
            .findByUsuarioIdAndStatus(usuarioAutenticado.id(), StatusParticipacao.ACEITO)
            .stream()
            .map(Participacao::getGrupo)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Grupo buscarPorId(UUID id) {
        Grupo disciplina = repository.findById(id)
            .orElseThrow(() -> new GrupoNaoEncontradoException(id));   // 404
        acesso.exigir(Acao.VER_CONTEUDO, id, "Somente participantes do grupo têm acesso a ele."); // 403
        return disciplina;
    }

    @Override
    @Transactional
    public Grupo atualizarDescricao(UUID id, String descricaoInformada) {
        Grupo disciplina = repository.findById(id)
            .orElseThrow(() -> new GrupoNaoEncontradoException(id));       // 404
        acesso.exigir(Acao.EDITAR_GRUPO, id, "Você não tem permissão para editar o grupo.");       // 403

        String descricao = descricaoInformada == null ? null : descricaoInformada.trim();
        if (descricao != null && descricao.length() > 500) {
            throw new RequisicaoInvalidaException("A descrição deve ter no máximo 500 caracteres."); // 400
        }
        // em branco, a disciplina fica sem descrição
        disciplina.setDescricao(descricao == null || descricao.isBlank() ? null : descricao);
        return repository.save(disciplina);
    }

    @Override
    @Transactional
    public void excluir(UUID id) {
        Grupo disciplina = repository.findById(id)
            .orElseThrow(() -> new GrupoNaoEncontradoException(id));   // 404
        acesso.exigir(Acao.EXCLUIR_GRUPO, id, "Você não tem permissão para excluir o grupo."); // 403, antes de apagar qualquer coisa

        // a cascata disciplina -> aulas -> anotações -> imagens (e aulas -> imagens avulsas) apaga os registros,
        // mas não os arquivos; as chaves são lidas antes e os arquivos só saem do disco depois que a exclusão é confirmada
        List<String> chavesDasImagens = new ArrayList<>(imagemRepository.findChavesArmazenamentoByGrupoId(id));
        chavesDasImagens.addAll(imagemEncontroRepository.findChavesArmazenamentoByGrupoId(id));
        // Remove os vínculos antes da disciplina para respeitar as FKs, sem composição JPA.
        participacaoRepository.deleteByGrupoId(id);
        recorrenciaRepository.deleteByGrupoId(id);
        repository.delete(disciplina);
        remocaoArquivos.removerAoConfirmar(chavesDasImagens);
    }
}