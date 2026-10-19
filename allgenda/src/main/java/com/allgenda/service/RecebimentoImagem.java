package com.allgenda.service;

import com.allgenda.exception.ArquivoMuitoGrandeException;
import com.allgenda.exception.RequisicaoInvalidaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

// Valida e grava uma imagem enviada, seja ela anexada a uma anotação ou avulsa na aula.
// Quem chama monta o registro com os dados devolvidos; se a transação for desfeita, o arquivo gravado é apagado.
@Component
public class RecebimentoImagem {

    private static final String NOME_PADRAO = "imagem";
    private static final int TAMANHO_MAXIMO_NOME = 255;

    private final ArmazenamentoService armazenamento;
    private final RemocaoArquivos remocaoArquivos;
    private final DataSize tamanhoMaximo;

    public RecebimentoImagem(ArmazenamentoService armazenamento,
                             RemocaoArquivos remocaoArquivos,
                             @Value("${app.uploads.tamanho-maximo}") DataSize tamanhoMaximo) {
        this.armazenamento = armazenamento;
        this.remocaoArquivos = remocaoArquivos;
        this.tamanhoMaximo = tamanhoMaximo;
    }

    // dados da imagem já gravada no armazenamento
    public record ImagemRecebida(String chave, String contentType, long tamanho, String nomeOriginal) {}

    // verificação barata, feita antes de qualquer regra que dependa do banco
    public void validarEnvio(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RequisicaoInvalidaException("Envie um arquivo de imagem.");
        }
        if (arquivo.getSize() > tamanhoMaximo.toBytes()) {
            throw new ArquivoMuitoGrandeException(tamanhoMaximo);
        }
    }

    public ImagemRecebida receber(MultipartFile arquivo) {
        validarEnvio(arquivo);

        byte[] conteudo = lerConteudo(arquivo);
        // o tipo vem dos primeiros bytes do arquivo; o Content-Type e a extensão enviados pelo cliente não são confiáveis
        TipoImagem tipo = TipoImagem.detectar(conteudo)
                .orElseThrow(() -> new RequisicaoInvalidaException("Formato não suportado. Envie uma imagem PNG, JPEG ou WebP."));

        String chave = armazenamento.salvar(conteudo, tipo.extensao);
        remocaoArquivos.removerSeDesfeita(chave);

        return new ImagemRecebida(chave, tipo.contentType, conteudo.length, nomeSeguro(arquivo.getOriginalFilename()));
    }

    private static byte[] lerConteudo(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler o arquivo enviado.", e);
        }
    }

    // o nome enviado só é exibido; tira o caminho e caracteres de controle e limita o tamanho
    static String nomeSeguro(String nomeOriginal) {
        if (nomeOriginal == null) {
            return NOME_PADRAO;
        }
        String nome = nomeOriginal.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").strip();
        if (nome.isEmpty()) {
            return NOME_PADRAO;
        }
        return nome.length() > TAMANHO_MAXIMO_NOME ? nome.substring(0, TAMANHO_MAXIMO_NOME) : nome;
    }

    private enum TipoImagem {
        PNG("image/png", "png"),
        JPEG("image/jpeg", "jpg"),
        WEBP("image/webp", "webp");

        private final String contentType;
        private final String extensao;

        TipoImagem(String contentType, String extensao) {
            this.contentType = contentType;
            this.extensao = extensao;
        }

        static Optional<TipoImagem> detectar(byte[] conteudo) {
            if (comecaCom(conteudo, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
                return Optional.of(PNG);
            }
            if (comecaCom(conteudo, 0, 0xFF, 0xD8, 0xFF)) {
                return Optional.of(JPEG);
            }
            if (comecaCom(conteudo, 0, 'R', 'I', 'F', 'F') && comecaCom(conteudo, 8, 'W', 'E', 'B', 'P')) {
                return Optional.of(WEBP);
            }
            return Optional.empty();
        }

        private static boolean comecaCom(byte[] conteudo, int inicio, int... esperados) {
            if (conteudo.length < inicio + esperados.length) {
                return false;
            }
            for (int i = 0; i < esperados.length; i++) {
                if ((conteudo[inicio + i] & 0xFF) != esperados[i]) {
                    return false;
                }
            }
            return true;
        }
    }
}
