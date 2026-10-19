package com.allgenda.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

// a validação e a detecção do tipo são cobertas pelos testes de ImagemAnotacaoServiceImpl, que usam esta classe
class RecebimentoImagemTest {

    @Test
    void nomeSeguroTiraCaminhoEControlesELimitaTamanho() {
        assertThat(RecebimentoImagem.nomeSeguro(null)).isEqualTo("imagem");
        assertThat(RecebimentoImagem.nomeSeguro("C:\\fotos\\quadro.png")).isEqualTo("quadro.png");
        assertThat(RecebimentoImagem.nomeSeguro("pasta/")).isEqualTo("imagem");
        assertThat(RecebimentoImagem.nomeSeguro("a\r\nb.png")).isEqualTo("ab.png");

        char[] longo = new char[300];
        Arrays.fill(longo, 'x');
        assertThat(RecebimentoImagem.nomeSeguro(new String(longo))).hasSize(255);
    }
}
