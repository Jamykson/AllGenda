package com.allgenda.service;

import com.allgenda.model.Disciplina;

import java.util.List;
import java.util.UUID;

public interface DisciplinaService {
    Disciplina cadastrar(DadosDoGrupo dados);
    List<Disciplina> listarDoUsuario();
    Disciplina buscarPorId(UUID id);
    // só o dono edita; por ora, só a descrição (ausente ou em branco: sem descrição)
    Disciplina atualizarDescricao(UUID id, String descricao);
    void excluir(UUID id);
}
