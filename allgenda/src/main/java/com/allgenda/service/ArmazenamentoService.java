package com.allgenda.service;

import java.util.Optional;

import org.springframework.core.io.Resource;

// Guarda os bytes de arquivos enviados. As implementações decidem onde (disco local, S3, ...); quem usa só conhece a chave.
public interface ArmazenamentoService {
    String salvar(byte[] conteudo, String extensao); // devolve a chave gerada para o arquivo
    Optional<Resource> carregar(String chave);       // vazio se o arquivo não existir
    void remover(String chave);                      // não faz nada se o arquivo já não existir
}
