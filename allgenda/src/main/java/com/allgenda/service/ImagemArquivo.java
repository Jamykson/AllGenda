package com.allgenda.service;

import org.springframework.core.io.Resource;

// conteúdo de uma imagem guardada e os dados que o controller precisa para montar a resposta
public record ImagemArquivo(Resource conteudo, String contentType, String nome) {}
