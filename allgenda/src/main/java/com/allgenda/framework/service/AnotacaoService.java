package com.allgenda.framework.service;

import com.allgenda.framework.model.Anotacao;
import com.allgenda.framework.model.VisibilidadeAnotacao;

import java.util.List;
import java.util.UUID;

public interface AnotacaoService {
    Anotacao cadastrar(NovaAnotacao nova);
    // acrescentar e remover tags é permitido a qualquer participante da disciplina, não só ao autor
    Anotacao associarTags(UUID anotacaoId, List<String> nomesTags);
    Anotacao removerTag(UUID anotacaoId, String nomeTag);
    List<Anotacao> listarPorAula(UUID aulaId);
    List<Anotacao> listarPorDisciplina(UUID disciplinaId, String tag); // tag opcional: null = todas
    List<Anotacao> listarPorTag(String tag);
    // substitui título e conteúdo: um campo ausente ou vazio fica sem valor, mas ao menos um dos dois é obrigatório
    Anotacao editar(UUID anotacaoId, String titulo, String conteudo);
    // torna a anotação privada (só do autor) ou a devolve ao grupo; só o autor altera
    Anotacao alterarVisibilidade(UUID anotacaoId, VisibilidadeAnotacao visibilidade);
    void excluir(UUID anotacaoId); // apaga também as imagens (registros e arquivos); só o autor pode excluir
    // anotações dos outros participantes da disciplina (exclui as do solicitante)
    List<Anotacao> listarDosColegas(UUID disciplinaId);
}
