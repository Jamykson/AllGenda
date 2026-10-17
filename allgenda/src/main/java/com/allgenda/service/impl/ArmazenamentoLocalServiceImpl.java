package com.allgenda.service.impl;

import com.allgenda.service.ArmazenamentoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.UUID;

// Guarda os arquivos num diretório do disco local, configurado em app.uploads.dir (variável UPLOADS_DIR).
@Service
public class ArmazenamentoLocalServiceImpl implements ArmazenamentoService {

    private final Path raiz;

    public ArmazenamentoLocalServiceImpl(@Value("${app.uploads.dir}") String diretorio) {
        this.raiz = Path.of(diretorio).toAbsolutePath().normalize();
        try {
            Files.createDirectories(raiz);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível criar o diretório de uploads: " + raiz, e);
        }
    }

    @Override
    public String salvar(byte[] conteudo, String extensao) {
        // o nome no disco é sempre gerado aqui, nunca vem do cliente
        String chave = UUID.randomUUID() + "." + extensao;
        try {
            Files.write(resolver(chave), conteudo, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível salvar o arquivo.", e);
        }
        return chave;
    }

    @Override
    public Optional<Resource> carregar(String chave) {
        Path arquivo = resolver(chave);
        return Files.isRegularFile(arquivo) ? Optional.of(new FileSystemResource(arquivo)) : Optional.empty();
    }

    @Override
    public void remover(String chave) {
        try {
            Files.deleteIfExists(resolver(chave));
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível remover o arquivo.", e);
        }
    }

    // impede que uma chave como "../algo" aponte para fora do diretório de uploads
    private Path resolver(String chave) {
        Path arquivo = raiz.resolve(chave).normalize();
        if (!raiz.equals(arquivo.getParent())) {
            throw new IllegalArgumentException("Chave de armazenamento inválida: " + chave);
        }
        return arquivo;
    }
}
