package com.allgenda.service.impl;

import com.allgenda.service.FalhaDoModeloException;
import com.allgenda.service.ModeloDeLinguagem;
import com.allgenda.service.PedidoAoModelo;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.RawMessageStreamEvent;
import com.anthropic.models.messages.TextBlockParam;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

// Gera as respostas do assistente com a Messages API da Anthropic, em streaming. Sem a chave
// (app.assistente.chave, vinda de ANTHROPIC_API_KEY), o back-end sobe normalmente e o assistente
// fica indisponível.
@Component
public class ModeloAnthropic implements ModeloDeLinguagem {

    private static final Logger log = LoggerFactory.getLogger(ModeloAnthropic.class);

    private final AnthropicClient cliente; // null sem chave
    private final String modelo;
    private final long maximoDeTokens;

    public ModeloAnthropic(@Value("${app.assistente.chave}") String chave,
                           @Value("${app.assistente.modelo}") String modelo,
                           @Value("${app.assistente.max-tokens}") long maximoDeTokens) {
        this.cliente = chave == null || chave.isBlank() ? null
                : AnthropicOkHttpClient.builder().apiKey(chave).timeout(Duration.ofMinutes(2)).build();
        this.modelo = modelo;
        this.maximoDeTokens = maximoDeTokens;
    }

    @Override
    public boolean configurado() {
        return cliente != null;
    }

    @Override
    public void responder(PedidoAoModelo pedido, Consumer<String> aoReceberTexto) {
        // o try fecha a conexão também quando aoReceberTexto falha (ex: o cliente desconectou)
        try (StreamResponse<RawMessageStreamEvent> resposta = cliente.messages().createStreaming(parametros(pedido))) {
            resposta.stream()
                    .flatMap(evento -> evento.contentBlockDelta().stream())
                    .flatMap(evento -> evento.delta().text().stream())
                    .forEach(delta -> aoReceberTexto.accept(delta.text()));
        }
        catch (AnthropicServiceException e) {
            log.warn("A API da Anthropic recusou o pedido do assistente (HTTP {}): {}", e.statusCode(), e.getMessage());
            throw new FalhaDoModeloException(mensagemPara(e.statusCode()), e);
        }
        catch (AnthropicIoException e) {
            log.warn("Falha de rede ao falar com a API da Anthropic: {}", e.getMessage());
            throw new FalhaDoModeloException("Não foi possível falar com o serviço do assistente. Tente de novo.", e);
        }
    }

    private static String mensagemPara(int status) {
        return switch (status) {
            case 429, 529 -> "O assistente está sobrecarregado no momento. Tente de novo em instantes.";
            case 401, 403 -> "O assistente está com a configuração inválida. Avise quem mantém o AllGenda.";
            case 413 -> "O material da aula ficou grande demais para o assistente.";
            default -> "Não foi possível gerar a resposta. Tente de novo.";
        };
    }

    private MessageCreateParams parametros(PedidoAoModelo pedido) {
        MessageCreateParams.Builder parametros = MessageCreateParams.builder()
                .model(modelo)
                .maxTokens(maximoDeTokens)
                .system(pedido.instrucoes());
        for (PedidoAoModelo.Mensagem mensagem : pedido.mensagens()) {
            List<ContentBlockParam> blocos = mensagem.blocos().stream().map(ModeloAnthropic::bloco).toList();
            parametros.addMessage(MessageParam.builder()
                    .role(mensagem.papel() == PedidoAoModelo.Papel.USUARIO ? MessageParam.Role.USER : MessageParam.Role.ASSISTANT)
                    .contentOfBlockParams(blocos)
                    .build());
        }
        return parametros.build();
    }

    private static ContentBlockParam bloco(PedidoAoModelo.Bloco bloco) {
        return switch (bloco) {
            case PedidoAoModelo.Texto texto -> {
                TextBlockParam.Builder construtor = TextBlockParam.builder().text(texto.texto());
                if (texto.fimDoMaterial()) {
                    construtor.cacheControl(CacheControlEphemeral.builder().build());
                }
                yield ContentBlockParam.ofText(construtor.build());
            }
            case PedidoAoModelo.Imagem imagem -> ContentBlockParam.ofImage(ImageBlockParam.builder()
                    .source(Base64ImageSource.builder()
                            .data(Base64.getEncoder().encodeToString(imagem.conteudo()))
                            .mediaType(Base64ImageSource.MediaType.of(imagem.contentType()))
                            .build())
                    .build());
        };
    }

    @PreDestroy
    void fechar() {
        if (cliente != null) {
            cliente.close();
        }
    }
}
