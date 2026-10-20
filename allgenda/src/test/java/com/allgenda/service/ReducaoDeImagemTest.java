package com.allgenda.service;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ReducaoDeImagemTest {

    private final ReducaoDeImagem reducao = new ReducaoDeImagem();

    private static byte[] png(int largura, int altura) throws IOException {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB), "png", saida);
        return saida.toByteArray();
    }

    @Test
    void imagemGrandeEhReduzidaParaOLadoMaximoEmJpeg() throws IOException {
        ReducaoDeImagem.ImagemPreparada preparada = reducao.preparar(png(4000, 3000), "image/png").orElseThrow();

        assertThat(preparada.contentType()).isEqualTo("image/jpeg");
        BufferedImage reduzida = ImageIO.read(new ByteArrayInputStream(preparada.conteudo()));
        assertThat(reduzida.getWidth()).isEqualTo(ReducaoDeImagem.LADO_MAXIMO);
        assertThat(reduzida.getHeight()).isEqualTo(1176); // mantém a proporção 4:3
    }

    @Test
    void imagemPequenaVaiComoEsta() throws IOException {
        byte[] original = png(800, 600);

        ReducaoDeImagem.ImagemPreparada preparada = reducao.preparar(original, "image/png").orElseThrow();

        assertThat(preparada.contentType()).isEqualTo("image/png");
        assertThat(preparada.conteudo()).isSameAs(original);
    }

    @Test
    void webpGrandeDemaisFicaDeFora() {
        byte[] grande = new byte[(int) ReducaoDeImagem.TAMANHO_MAXIMO + 1];

        assertThat(reducao.preparar(grande, "image/webp")).isEmpty();
        assertThat(reducao.preparar(new byte[100], "image/webp")).isPresent();
    }
}
