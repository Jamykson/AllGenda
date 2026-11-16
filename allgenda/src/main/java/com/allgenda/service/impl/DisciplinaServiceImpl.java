package com.allgenda.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.allgenda.exception.DisciplinaNaoEncontradaException;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.model.Disciplina;
import com.allgenda.model.PapelParticipacao;
import com.allgenda.model.Participacao;
import com.allgenda.model.StatusParticipacao;
import com.allgenda.repository.DisciplinaRepository;
import com.allgenda.repository.ParticipacaoRepository;
import com.allgenda.repository.RecorrenciaRepository;
import com.allgenda.repository.ImagemAnotacaoRepository;
import com.allgenda.repository.ImagemAulaRepository;
import com.allgenda.service.AcessoDisciplina;
import com.allgenda.service.DadosDoGrupo;
import com.allgenda.service.DisciplinaService;
import com.allgenda.service.RemocaoArquivos;
import com.allgenda.service.UsuarioAutenticado;

@Service
public class DisciplinaServiceImpl implements DisciplinaService {

    private final DisciplinaRepository repository;
    private final ParticipacaoRepository participacaoRepository;
    private final RecorrenciaRepository recorrenciaRepository;
    private final ImagemAnotacaoRepository imagemRepository;
    private final ImagemAulaRepository imagemAulaRepository;
    private final RemocaoArquivos remocaoArquivos;
    private final UsuarioAutenticado usuarioAutenticado;
    private final AcessoDisciplina acesso;

    public DisciplinaServiceImpl(DisciplinaRepository repository,
                                 ParticipacaoRepository participacaoRepository,
                                 RecorrenciaRepository recorrenciaRepository,
                                 ImagemAnotacaoRepository imagemRepository,
                                 ImagemAulaRepository imagemAulaRepository,
                                 RemocaoArquivos remocaoArquivos,
                                 UsuarioAutenticado usuarioAutenticado,
                                 AcessoDisciplina acesso){
        this.repository = repository;
        this.participacaoRepository = participacaoRepository;
        this.recorrenciaRepository = recorrenciaRepository;
        this.imagemRepository = imagemRepository;
        this.imagemAulaRepository = imagemAulaRepository;
        this.remocaoArquivos = remocaoArquivos;
        this.usuarioAutenticado = usuarioAutenticado;
        this.acesso = acesso;
    }

    @Override
    @Transactional
    public Disciplina cadastrar(DadosDoGrupo dto) {
        if(dto.nome() == null || dto.nome().isBlank()){
            throw new RequisicaoInvalidaException("O nome da disciplina é obrigatório.");
        }

        String nome = dto.nome().trim();
        String descricao = dto.descricao() == null ? null : dto.descricao().trim();

        if (descricao != null && descricao.length() > 500) {
            throw new RequisicaoInvalidaException("A descrição deve ter no máximo 500 caracteres.");
        }

        Disciplina disciplina = new Disciplina();
        disciplina.setNome(nome);
        disciplina.setDescricao(descricao == null || descricao.isBlank() ? null : descricao);

        // quem cria a disciplina vira o dono, com a participação já aceita.
        Participacao participacao = new Participacao();
        participacao.setUsuario(usuarioAutenticado.obter());
        participacao.setPapel(PapelParticipacao.DONO);
        participacao.setStatus(StatusParticipacao.ACEITO);
        Disciplina salva = repository.save(disciplina);
        salva.addParticipacao(participacao);
        // A associação não usa cascata: disciplina e dono são gravados na mesma transação.
        participacaoRepository.save(participacao);
        return salva;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Disciplina> listarDoUsuario() {
        // só as disciplinas em que a pessoa participa com status ACEITO (dono ou membro)
        return participacaoRepository
            .findByUsuarioIdAndStatus(usuarioAutenticado.id(), StatusParticipacao.ACEITO)
            .stream()
            .map(Participacao::getDisciplina)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Disciplina buscarPorId(UUID id) {
        Disciplina disciplina = repository.findById(id)
            .orElseThrow(() -> new DisciplinaNaoEncontradaException(id));   // 404
        acesso.exigirParticipanteAceito(id);                                 // 403
        return disciplina;
    }

    @Override
    @Transactional
    public Disciplina atualizarDescricao(UUID id, String descricaoInformada) {
        Disciplina disciplina = repository.findById(id)
            .orElseThrow(() -> new DisciplinaNaoEncontradaException(id));       // 404
        acesso.exigirDono(id, "Somente o dono pode editar a disciplina.");       // 403

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
        Disciplina disciplina = repository.findById(id)
            .orElseThrow(() -> new DisciplinaNaoEncontradaException(id));   // 404
        acesso.exigirDono(id, "Somente o dono pode excluir a disciplina."); // 403, antes de apagar qualquer coisa

        // a cascata disciplina -> aulas -> anotações -> imagens (e aulas -> imagens avulsas) apaga os registros,
        // mas não os arquivos; as chaves são lidas antes e os arquivos só saem do disco depois que a exclusão é confirmada
        List<String> chavesDasImagens = new ArrayList<>(imagemRepository.findChavesArmazenamentoByDisciplinaId(id));
        chavesDasImagens.addAll(imagemAulaRepository.findChavesArmazenamentoByDisciplinaId(id));
        // Remove os vínculos antes da disciplina para respeitar as FKs, sem composição JPA.
        participacaoRepository.deleteByDisciplinaId(id);
        recorrenciaRepository.deleteByDisciplinaId(id);
        repository.delete(disciplina);
        remocaoArquivos.removerAoConfirmar(chavesDasImagens);
    }
}