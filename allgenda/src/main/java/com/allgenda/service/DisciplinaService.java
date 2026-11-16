package com.allgenda.service;

import com.allgenda.model.Grupo;

import java.util.List;
import java.util.UUID;

public interface DisciplinaService {
    Grupo cadastrar(DadosDoGrupo dados);
    List<Grupo> listarDoUsuario();
    Grupo buscarPorId(UUID id);
    // só o dono edita; por ora, só a descrição (ausente ou em branco: sem descrição)
    Grupo atualizarDescricao(UUID id, String descricao);
    void excluir(UUID id);
}
