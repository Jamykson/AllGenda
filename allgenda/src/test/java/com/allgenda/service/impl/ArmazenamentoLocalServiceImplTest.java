package com.allgenda.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArmazenamentoLocalServiceImplTest {

    @TempDir
    Path diretorio;

    private ArmazenamentoLocalServiceImpl armazenamento;

    @BeforeEach
    void setUp() {
        armazenamento = new ArmazenamentoLocalServiceImpl(diretorio.resolve("uploads").toString());
    }

    @Test
    void salvaECarregaOsMesmosBytes() throws Exception {
        byte[] conteudo = {1, 2, 3};

        String chave = armazenamento.salvar(conteudo, "png");

        assertThat(chave).endsWith(".png");
        assertThat(armazenamento.carregar(chave)).hasValueSatisfying(recurso ->
                assertThat(recurso).satisfies(r -> assertThat(r.getContentAsByteArray()).isEqualTo(conteudo)));
    }

    @Test
    void chavesSaoDiferentesParaCadaArquivo() {
        assertThat(armazenamento.salvar(new byte[]{1}, "png")).isNotEqualTo(armazenamento.salvar(new byte[]{1}, "png"));
    }

    @Test
    void carregarArquivoInexistenteDevolveVazio() {
        assertThat(armazenamento.carregar("nao-existe.png")).isEmpty();
    }

    @Test
    void removerApagaOArquivoEPodeSerRepetido() {
        String chave = armazenamento.salvar(new byte[]{1}, "png");

        armazenamento.remover(chave);
        armazenamento.remover(chave);

        assertThat(armazenamento.carregar(chave)).isEmpty();
    }

    @Test
    void chaveQueSaiDoDiretorioEhRecusada() throws Exception {
        Files.writeString(diretorio.resolve("segredo.txt"), "não pode ser lido");

        assertThatThrownBy(() -> armazenamento.carregar("../segredo.txt")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> armazenamento.remover("../segredo.txt")).isInstanceOf(IllegalArgumentException.class);
        assertThat(diretorio.resolve("segredo.txt")).exists();
    }
}
