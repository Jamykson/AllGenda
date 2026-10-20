package com.allgenda;

import com.allgenda.dto.request.LoginRequestDTO;
import com.allgenda.dto.request.UsuarioRequestDTO;
import com.allgenda.service.AuthService;
import com.allgenda.service.FalhaDoModeloException;
import com.allgenda.service.ModeloDeLinguagem;
import com.allgenda.service.PedidoAoModelo;
import com.allgenda.service.UsuarioService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// O assistente responde por SSE usando o material da aula. O modelo de linguagem é falso: registra
// o pedido que recebeu e responde em dois pedaços, ou falha quando a pergunta pede.
@SpringBootTest
class AssistenteIntegrationTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};

    static class ModeloFalso implements ModeloDeLinguagem {
        final List<PedidoAoModelo> pedidos = new ArrayList<>();

        @Override
        public boolean configurado() {
            return true;
        }

        @Override
        public void responder(PedidoAoModelo pedido, Consumer<String> aoReceberTexto) {
            pedidos.add(pedido);
            aoReceberTexto.accept("Resumo ");
            if (ultimoTexto(pedido).contains("falhe")) {
                throw new FalhaDoModeloException("O assistente está sobrecarregado no momento.", null);
            }
            aoReceberTexto.accept("da aula.");
        }
    }

    @TestConfiguration
    static class Configuracao {
        @Bean
        @Primary
        ModeloFalso modeloFalso() {
            return new ModeloFalso();
        }
    }

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy filtros;
    @Autowired AuthService auth;
    @Autowired UsuarioService cadastroUsuarios;
    @Autowired ModeloFalso modelo;

    private MockMvc mvc;
    private String tokenAutor;
    private String tokenColega;
    private String tokenDeFora;
    private String aulaId;
    private String outraAulaId;
    private String anotacaoId;
    private String imagemId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filtros).build();
        modelo.pedidos.clear();

        String sufixo = UUID.randomUUID().toString();
        tokenAutor = cadastrarELogar("Autor", sufixo);
        tokenColega = cadastrarELogar("Colega", sufixo);
        tokenDeFora = cadastrarELogar("Visitante", sufixo);

        String disciplinaId = criar("/api/disciplinas", "{\"nome\":\"PDS\"}", tokenAutor);
        String conviteId = criar("/api/disciplinas/" + disciplinaId + "/convites", "{\"email\":\"colega-" + sufixo + "@example.com\"}", tokenAutor);
        mvc.perform(comToken(patch("/api/convites/{id}/aceitar", conviteId), tokenColega)).andExpect(status().isOk());

        aulaId = criar("/api/aulas", "{\"disciplinaId\":\"" + disciplinaId
                + "\",\"data\":\"2026-10-20\",\"horario\":\"08:00\",\"topico\":\"Padrões de projeto\"}", tokenAutor);
        outraAulaId = criar("/api/aulas", "{\"disciplinaId\":\"" + disciplinaId
                + "\",\"data\":\"2026-10-22\",\"horario\":\"08:00\",\"topico\":\"Outra\"}", tokenAutor);

        anotacaoId = criar("/api/anotacoes", "{\"aulaId\":\"" + aulaId
                + "\",\"titulo\":\"Padrões GoF\",\"conteudo\":\"Observer usa List<Listener>\",\"tags\":[\"Prova\"]}", tokenAutor);
        criar("/api/anotacoes", "{\"aulaId\":\"" + aulaId + "\",\"conteudo\":\"Singleton é controverso\"}", tokenColega);

        String resposta = mvc.perform(comToken(multipart("/api/aulas/{id}/imagens", aulaId)
                        .file(new MockMultipartFile("arquivo", "quadro.png", "image/png", PNG))
                        .param("legenda", "Diagrama do Observer"), tokenColega))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        imagemId = JsonPath.read(resposta, "$.id");
    }

    private String cadastrarELogar(String nome, String sufixo) {
        var usuario = cadastroUsuarios.cadastrar(new UsuarioRequestDTO(nome + " " + sufixo,
                nome.toLowerCase() + "-" + sufixo + "@example.com", "senha-forte-123"));
        return auth.login(new LoginRequestDTO(usuario.email(), "senha-forte-123")).token();
    }

    private static RequestBuilder comToken(AbstractMockHttpServletRequestBuilder<?> requisicao, String token) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String criar(String caminho, String json, String token) throws Exception {
        String resposta = mvc.perform(comToken(post(caminho).contentType(MediaType.APPLICATION_JSON).content(json), token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private RequestBuilder pergunta(String aula, String mensagensJson, String token) {
        return comToken(post("/api/aulas/{id}/assistente", aula)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_PROBLEM_JSON)
                .content("{\"mensagens\":" + mensagensJson + "}"), token);
    }

    // abre o stream e devolve o corpo SSE completo
    private String respostaCompleta(RequestBuilder pergunta) throws Exception {
        MvcResult iniciado = mvc.perform(pergunta).andExpect(request().asyncStarted()).andReturn();
        return mvc.perform(asyncDispatch(iniciado))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8); // SSE é sempre UTF-8
    }

    private static String ultimoTexto(PedidoAoModelo pedido) {
        List<PedidoAoModelo.Bloco> blocos = pedido.mensagens().getLast().blocos();
        return ((PedidoAoModelo.Texto) blocos.getLast()).texto();
    }

    private static String textoDoMaterial(PedidoAoModelo pedido) {
        StringBuilder texto = new StringBuilder();
        for (PedidoAoModelo.Bloco bloco : pedido.mensagens().getFirst().blocos()) {
            if (bloco instanceof PedidoAoModelo.Texto t) {
                texto.append(t.texto()).append('\n');
            }
        }
        return texto.toString();
    }

    @Test
    void participanteRecebeARespostaEmPedacosComOMaterialDaAula() throws Exception {
        String corpo = respostaCompleta(pergunta(aulaId, "[{\"papel\":\"USUARIO\",\"texto\":\"Resuma a aula\"}]", tokenColega));

        assertThat(corpo).containsSubsequence(
                "event:texto", "data:{\"texto\":\"Resumo \"}",
                "event:texto", "data:{\"texto\":\"da aula.\"}",
                "event:fim");

        PedidoAoModelo pedido = modelo.pedidos.getFirst();
        assertThat(pedido.instrucoes()).contains("PDS", "20/10/2026", "Padrões de projeto");
        String material = textoDoMaterial(pedido);
        // as anotações de todos os participantes, com o texto escapado
        assertThat(material).contains("titulo=\"Padrões GoF\"", "Observer usa List&lt;Listener&gt;", "tags=\"Prova\"",
                "Singleton é controverso", "Imagem 1 (foto avulsa da galeria da aula", "legenda: \"Diagrama do Observer\"");
        assertThat(pedido.mensagens().getFirst().blocos()).filteredOn(PedidoAoModelo.Imagem.class::isInstance).hasSize(1);
        assertThat(ultimoTexto(pedido)).isEqualTo("Resuma a aula");
    }

    @Test
    void conversaComFocoReenviaOHistoricoEIndicaOItem() throws Exception {
        respostaCompleta(pergunta(aulaId, """
                [{"papel":"USUARIO","texto":"Resuma a aula"},
                 {"papel":"ASSISTENTE","texto":"Resumo da aula."},
                 {"papel":"USUARIO","texto":"O que mostra?","foco":{"tipo":"IMAGEM","id":"%s"}}]
                """.formatted(imagemId), tokenAutor));
        respostaCompleta(pergunta(aulaId, """
                [{"papel":"USUARIO","texto":"Explique","foco":{"tipo":"ANOTACAO","id":"%s"}}]
                """.formatted(anotacaoId), tokenAutor));

        PedidoAoModelo comHistorico = modelo.pedidos.get(0);
        assertThat(comHistorico.mensagens()).extracting(PedidoAoModelo.Mensagem::papel).containsExactly(
                PedidoAoModelo.Papel.USUARIO, PedidoAoModelo.Papel.ASSISTENTE, PedidoAoModelo.Papel.USUARIO);
        assertThat(ultimoTexto(comHistorico)).isEqualTo("[Pergunta sobre a imagem 1]\nO que mostra?");
        assertThat(ultimoTexto(modelo.pedidos.get(1))).startsWith("[Pergunta sobre a anotação 1 (\"Padrões GoF\", de Autor ");
    }

    @Test
    void falhaDoModeloNoMeioDoCaminhoViraEventoDeErro() throws Exception {
        String corpo = respostaCompleta(pergunta(aulaId, "[{\"papel\":\"USUARIO\",\"texto\":\"falhe, por favor\"}]", tokenAutor));

        assertThat(corpo).containsSubsequence("event:texto", "event:erro", "O assistente está sobrecarregado no momento.");
        assertThat(corpo).doesNotContain("event:fim");
    }

    @Test
    void errosSaemAntesDoStreamComoProblemDetail() throws Exception {
        String pergunta = "[{\"papel\":\"USUARIO\",\"texto\":\"Resuma\"}]";
        mvc.perform(pergunta(aulaId, pergunta, tokenDeFora))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Somente participantes da disciplina usam o assistente da aula."));
        mvc.perform(pergunta(UUID.randomUUID().toString(), pergunta, tokenAutor))
                .andExpect(status().isNotFound());
        mvc.perform(pergunta(aulaId, "[{\"papel\":\"ASSISTENTE\",\"texto\":\"Olá\"}]", tokenAutor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "A conversa deve alternar perguntas e respostas, começando e terminando com uma pergunta."));
        mvc.perform(pergunta(aulaId, "[]", tokenAutor))
                .andExpect(status().isBadRequest());

        // a anotação é de outra aula
        String deOutraAula = criar("/api/anotacoes", "{\"aulaId\":\"" + outraAulaId + "\",\"conteudo\":\"Outra\"}", tokenAutor);
        mvc.perform(pergunta(aulaId, """
                [{"papel":"USUARIO","texto":"Explique","foco":{"tipo":"ANOTACAO","id":"%s"}}]
                """.formatted(deOutraAula), tokenAutor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O item em foco não pertence a esta aula."));

        mvc.perform(post("/api/aulas/{id}/assistente", aulaId).contentType(MediaType.APPLICATION_JSON).content("{\"mensagens\":" + pergunta + "}"))
                .andExpect(status().isUnauthorized());
        assertThat(modelo.pedidos).isEmpty();
    }
}
