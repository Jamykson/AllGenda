package com.allgenda.framework.service;

import com.allgenda.framework.extensao.DadosDoGrupo;
import com.allgenda.framework.model.Grupo;

import java.util.List;
import java.util.UUID;

public interface GrupoService {
    // cria um grupo do tipo registrado pela aplicação; quem cria recebe o papel de criador da política
    Grupo criar(String tipo, DadosDoGrupo dados);
    // os grupos em que a pessoa logada tem participação ACEITO
    List<Grupo> listarDoUsuario();
    Grupo buscarPorId(UUID id);
    // por ora só a descrição muda (ausente ou em branco: sem descrição)
    Grupo atualizarDescricao(UUID id, String descricao);
    // apaga o grupo e tudo o que é dele, inclusive participações, recorrência e arquivos das imagens
    void excluir(UUID id);
}
