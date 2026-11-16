package com.allgenda.framework;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

// Regra de arquitetura do framework: o núcleo nunca depende de uma aplicação. É a aplicação que conhece o
// núcleo (pelas interfaces de extensão e pelos services), e o núcleo a chama sem saber quem ela é.
class NucleoNaoDependeDaAplicacaoTest {

    private static final Path NUCLEO = Path.of("src/main/java/com/allgenda/framework");

    @Test
    void nenhumArquivoDoNucleoUsaOPacoteDasAplicacoes() throws IOException {
        assertThat(NUCLEO).isDirectory();
        try (Stream<Path> arquivos = Files.walk(NUCLEO)) {
            List<Path> fontes = arquivos.filter(arquivo -> arquivo.toString().endsWith(".java")).toList();
            assertThat(fontes).isNotEmpty();
            List<Path> violacoes = fontes.stream().filter(NucleoNaoDependeDaAplicacaoTest::usaAAplicacao).toList();
            assertThat(violacoes).as("arquivos do núcleo que dependem de com.allgenda.aplicacao").isEmpty();
        }
    }

    // pega o import e também um nome qualificado no meio do código
    private static boolean usaAAplicacao(Path arquivo) {
        try {
            return Files.readString(arquivo).contains("com.allgenda.aplicacao");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
