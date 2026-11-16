package com.allgenda.service;

import com.allgenda.framework.extensao.DadosDoEncontro;
import com.allgenda.model.Encontro;

import java.util.List;
import java.util.UUID;

public interface EncontroService {
    // cria um encontro do tipo registrado pela aplicação
    Encontro criar(String tipo, DadosDoEncontro dados);
    List<Encontro> listarPorGrupo(UUID grupoId);
    Encontro buscarPorId(UUID id);
    // edita data, horário e tópico e marca ou desmarca o cancelamento (o que vier nulo não muda)
    Encontro atualizar(UUID id, AtualizacaoDeEncontro atualizacao);
    // apaga também as anotações (inclusive privadas) e as imagens, com os arquivos
    void excluir(UUID id);
}
