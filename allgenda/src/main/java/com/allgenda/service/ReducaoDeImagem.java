package com.allgenda.service;

import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

// Prepara as imagens da aula para o modelo de linguagem. O upload aceita até 5 MB, mas a API aceita
// no máximo 5 MB depois da codificação em base64 (uns 3,75 MB de arquivo), e o modelo reduz para
// 1568 px no maior lado de qualquer forma. PNG e JPEG maiores que isso são reduzidos e regravados
// em JPEG; WebP, que o ImageIO não lê, vai como está se couber.
@Component
public class ReducaoDeImagem {

    public static final int LADO_MAXIMO = 1568;
    static final long TAMANHO_MAXIMO = 3_750_000; // 5 MB em base64
    private static final long TAMANHO_PARA_REGRAVAR = 3_500_000;
    private static final float QUALIDADE_JPEG = 0.85f;

    public record ImagemPreparada(byte[] conteudo, String contentType) {}

    // vazio quando a imagem não pode ser enviada (grande demais e impossível de reduzir)
    public Optional<ImagemPreparada> preparar(byte[] conteudo, String contentType) {
        if ("image/png".equals(contentType) || "image/jpeg".equals(contentType)) {
            BufferedImage imagem = ler(conteudo);
            if (imagem != null && (maiorLado(imagem) > LADO_MAXIMO || conteudo.length > TAMANHO_PARA_REGRAVAR)) {
                return Optional.of(new ImagemPreparada(regravarEmJpeg(imagem), "image/jpeg"));
            }
        }
        if (conteudo.length > TAMANHO_MAXIMO) {
            return Optional.empty();
        }
        return Optional.of(new ImagemPreparada(conteudo, contentType));
    }

    private static BufferedImage ler(byte[] conteudo) {
        try {
            return ImageIO.read(new ByteArrayInputStream(conteudo));
        }
        catch (IOException e) {
            return null;
        }
    }

    private static int maiorLado(BufferedImage imagem) {
        return Math.max(imagem.getWidth(), imagem.getHeight());
    }

    private static byte[] regravarEmJpeg(BufferedImage original) {
        double escala = Math.min(1.0, (double) LADO_MAXIMO / maiorLado(original));
        int largura = Math.max(1, (int) Math.round(original.getWidth() * escala));
        int altura = Math.max(1, (int) Math.round(original.getHeight() * escala));

        // JPEG não tem transparência: o fundo transparente de um PNG vira branco
        BufferedImage reduzida = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D grafico = reduzida.createGraphics();
        try {
            grafico.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            grafico.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            grafico.drawImage(original, 0, 0, largura, altura, Color.WHITE, null);
        }
        finally {
            grafico.dispose();
        }

        ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam parametros = escritor.getDefaultWriteParam();
        parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        parametros.setCompressionQuality(QUALIDADE_JPEG);
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        try (ImageOutputStream destino = ImageIO.createImageOutputStream(saida)) {
            escritor.setOutput(destino);
            escritor.write(null, new IIOImage(reduzida, null, null), parametros);
        }
        catch (IOException e) {
            throw new IllegalStateException("Não foi possível regravar a imagem em JPEG.", e);
        }
        finally {
            escritor.dispose();
        }
        return saida.toByteArray();
    }
}
