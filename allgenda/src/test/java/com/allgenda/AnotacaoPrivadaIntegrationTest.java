package com.allgenda;

import com.allgenda.dto.request.LoginRequestDTO;
import com.allgenda.dto.request.UsuarioRequestDTO;
import com.allgenda.service.AuthService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A anotação privada só existe para o autor. Este teste percorre todas as portas por onde uma anotação
// pode ser lida ou alterada (listagens, tags, edição, imagens e assistente) e confere que nenhuma delas
// a mostra, nem revela que ela existe: para os outros, o id da privada dá 404, como um id inventado.
@SpringBootTest
class AnotacaoPrivadaIntegrationTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};
    private static final String SEGREDO = "segredo-so-do-autor";

    private record Pessoa(UUID id, String email, String token) {}

    static class ModeloFalso implements ModeloDeLinguagem {
        final List<PedidoAoModelo> pedidos = new ArrayList<>();

        @Override
        public boolean configurado() {
            return true;
        }

        @Override
        public void responder(PedidoAoModelo pedido, Consumer<String> aoReceberTexto) {
            pedidos.add(pedido);
            aoReceberTexto.accept("ok");
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
    private Pessoa autor;
    private Pessoa colega;
    private Pessoa estranho;
    private String disciplinaId;
    private String aulaId;
    private String privada;
    private String doGrupo;
    private String imagemDaPrivada;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filtros).build();
        modelo.pedidos.clear();
        autor = criarPessoa("Autor");
        colega = criarPessoa("Colega");
        estranho = criarPessoa("Estranho");

        disciplinaId = criar("/api/disciplinas", "{\"nome\":\"PDS\"}", autor);
        String conviteId = criar("/api/disciplinas/" + disciplinaId + "/convites", "{\"email\":\"" + colega.email() + "\"}", autor);
        mvc.perform(comToken(patch("/api/convites/{id}/aceitar", conviteId), colega)).andExpect(status().isOk());
        aulaId = criar("/api/aulas", "{\"disciplinaId\":\"" + disciplinaId
                + "\",\"data\":\"2026-10-19\",\"horario\":\"08:00\",\"topico\":\"Strategy\"}", autor);

        privada = criar("/api/anotacoes", "{\"aulaId\":\"" + aulaId + "\",\"conteudo\":\"" + SEGREDO
                + "\",\"tags\":[\"java\"],\"visibilidade\":\"PRIVADA\"}", autor);
        doGrupo = criar("/api/anotacoes", "{\"aulaId\":\"" + aulaId + "\",\"conteudo\":\"Do grupo\",\"tags\":[\"java\"]}", autor);

        String resposta = mvc.perform(comToken(multipart("/api/anotacoes/{id}/imagens", privada)
                        .file(new MockMultipartFile("arquivo", "quadro.png", "image/png", PNG)), autor))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        imagemDaPrivada = JsonPath.read(resposta, "$.id");
    }

    private Pessoa criarPessoa(String prefixo) {
        String sufixo = UUID.randomUUID().toString();
        String email = prefixo.toLowerCase() + "-" + sufixo + "@example.com";
        var usuario = cadastroUsuarios.cadastrar(new UsuarioRequestDTO(prefixo + " " + sufixo, email, "senha-forte-123"));
        String token = auth.login(new LoginRequestDTO(email, "senha-forte-123")).token();
        return new Pessoa(usuario.id(), email, token);
    }

    private RequestBuilder comToken(AbstractMockHttpServletRequestBuilder<?> requisicao, Pessoa quem) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + quem.token());
    }

    private String criar(String caminho, String json, Pessoa quem) throws Exception {
        String resposta = mvc.perform(comToken(post(caminho).contentType(MediaType.APPLICATION_JSON).content(json), quem))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private RequestBuilder json(AbstractMockHttpServletRequestBuilder<?> requisicao, String corpo, Pessoa quem) {
        return comToken(requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo), quem);
    }

    @Test
    void oAutorVeASuaAnotacaoPrivadaEOGrupoNao() throws Exception {
        mvc.perform(comToken(get("/api/anotacoes").param("aulaId", aulaId), autor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(privada, doGrupo)))
                .andExpect(jsonPath("$[?(@.id=='" + privada + "')].visibilidade").value("PRIVADA"));
        mvc.perform(comToken(get("/api/anotacoes").param("aulaId", aulaId), colega))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(doGrupo)))
                .andExpect(jsonPath("$[0].visibilidade").value("GRUPO"));
    }

    @Test
    void asListagensPorDisciplinaTagEColegasNaoMostramAPrivada() throws Exception {
        mvc.perform(comToken(get("/api/anotacoes").param("disciplinaId", disciplinaId), colega))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].id", containsInAnyOrder(doGrupo)));
        mvc.perform(comToken(get("/api/anotacoes").param("disciplinaId", disciplinaId).param("tag", "java"), colega))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].id", containsInAnyOrder(doGrupo)));
        mvc.perform(comToken(get("/api/anotacoes").param("tag", "java"), colega))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].id", containsInAnyOrder(doGrupo)));
        mvc.perform(comToken(get("/api/disciplinas/{id}/anotacoes/colegas", disciplinaId), colega))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].id", containsInAnyOrder(doGrupo)));

        // para o autor, a privada aparece nas listagens dele
        mvc.perform(comToken(get("/api/anotacoes").param("disciplinaId", disciplinaId).param("tag", "java"), autor))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].id", containsInAnyOrder(privada, doGrupo)));
    }

    @Test
    void porIdAPrivadaDeOutraPessoaDa404ParaTudoInclusiveParaQuemNaoParticipa() throws Exception {
        String corpo = "{\"titulo\":\"x\",\"conteudo\":\"y\"}";
        for (Pessoa quem : List.of(colega, estranho)) {
            mvc.perform(json(patch("/api/anotacoes/{id}", privada), corpo, quem)).andExpect(status().isNotFound());
            mvc.perform(comToken(delete("/api/anotacoes/{id}", privada), quem)).andExpect(status().isNotFound());
            mvc.perform(json(post("/api/anotacoes/{id}/tags", privada), "[\"nova\"]", quem)).andExpect(status().isNotFound());
            mvc.perform(comToken(delete("/api/anotacoes/{id}/tags", privada).param("nome", "java"), quem)).andExpect(status().isNotFound());
            mvc.perform(json(patch("/api/anotacoes/{id}/visibilidade", privada), "{\"visibilidade\":\"GRUPO\"}", quem))
                    .andExpect(status().isNotFound());
            mvc.perform(comToken(get("/api/anotacoes/{a}/imagens/{i}", privada, imagemDaPrivada), quem)).andExpect(status().isNotFound());
            mvc.perform(comToken(multipart("/api/anotacoes/{id}/imagens", privada)
                            .file(new MockMultipartFile("arquivo", "x.png", "image/png", PNG)), quem))
                    .andExpect(status().isNotFound());
            mvc.perform(comToken(delete("/api/anotacoes/{a}/imagens/{i}", privada, imagemDaPrivada), quem)).andExpect(status().isNotFound());
        }
        // nada mudou para o autor
        mvc.perform(comToken(get("/api/anotacoes/{a}/imagens/{i}", privada, imagemDaPrivada), autor)).andExpect(status().isOk());
        mvc.perform(comToken(get("/api/anotacoes").param("aulaId", aulaId), autor))
                .andExpect(jsonPath("$[?(@.id=='" + privada + "')].conteudo").value(SEGREDO))
                .andExpect(jsonPath("$[?(@.id=='" + privada + "')].tags[0]").value("java"));
    }

    @Test
    void oAutorAlternaAVisibilidadeEOGrupoPassaAVer() throws Exception {
        mvc.perform(json(patch("/api/anotacoes/{id}/visibilidade", privada), "{\"visibilidade\":\"GRUPO\"}", autor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibilidade").value("GRUPO"));
        mvc.perform(comToken(get("/api/anotacoes").param("aulaId", aulaId), colega))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(privada, doGrupo)));
        mvc.perform(comToken(get("/api/anotacoes/{a}/imagens/{i}", privada, imagemDaPrivada), colega)).andExpect(status().isOk());

        // e de volta: some de novo para o colega
        mvc.perform(json(patch("/api/anotacoes/{id}/visibilidade", privada), "{\"visibilidade\":\"PRIVADA\"}", autor))
                .andExpect(status().isOk());
        mvc.perform(comToken(get("/api/anotacoes").param("aulaId", aulaId), colega))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(doGrupo)));
    }

    @Test
    void soOAutorAlteraAVisibilidadeDeUmaAnotacaoDoGrupo() throws Exception {
        mvc.perform(json(patch("/api/anotacoes/{id}/visibilidade", doGrupo), "{\"visibilidade\":\"PRIVADA\"}", colega))
                .andExpect(status().isForbidden());
        mvc.perform(json(patch("/api/anotacoes/{id}/visibilidade", doGrupo), "{}", autor))
                .andExpect(status().isBadRequest());
        mvc.perform(json(patch("/api/anotacoes/{id}/visibilidade", UUID.randomUUID()), "{\"visibilidade\":\"GRUPO\"}", autor))
                .andExpect(status().isNotFound());
        mvc.perform(comToken(get("/api/anotacoes").param("aulaId", aulaId), colega))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(doGrupo)));
    }

    @Test
    void oAssistenteNuncaLeAPrivadaNemParaOAutorENemAceitaEla() throws Exception {
        String pergunta = "{\"mensagens\":[{\"papel\":\"USUARIO\",\"texto\":\"Resuma\"}]}";
        for (Pessoa quem : List.of(autor, colega)) {
            MvcResult iniciado = mvc.perform(comToken(post("/api/aulas/{id}/assistente", aulaId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_PROBLEM_JSON)
                            .content(pergunta), quem))
                    .andExpect(request().asyncStarted()).andReturn();
            mvc.perform(asyncDispatch(iniciado)).andExpect(status().isOk());
        }
        assertThat(modelo.pedidos).hasSize(2);
        for (PedidoAoModelo pedido : modelo.pedidos) {
            StringBuilder material = new StringBuilder();
            for (PedidoAoModelo.Bloco bloco : pedido.mensagens().getFirst().blocos()) {
                if (bloco instanceof PedidoAoModelo.Texto texto) {
                    material.append(texto.texto());
                }
            }
            assertThat(material.toString()).contains("Do grupo").doesNotContain(SEGREDO);
            assertThat(pedido.mensagens().getFirst().blocos()).noneMatch(PedidoAoModelo.Imagem.class::isInstance); // a imagem da privada
        }

        // pôr a privada em foco também é recusado: para o assistente, ela não existe
        mvc.perform(comToken(post("/api/aulas/{id}/assistente", aulaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_PROBLEM_JSON)
                        .content("{\"mensagens\":[{\"papel\":\"USUARIO\",\"texto\":\"Explique\",\"foco\":{\"tipo\":\"ANOTACAO\",\"id\":\""
                                + privada + "\"}}]}"), autor))
                .andExpect(status().isBadRequest());
    }

    @Test
    void naoMudaNadaParaQuemNaoUsaAPrivacidade() throws Exception {
        // sem o campo, a anotação é do grupo; um valor inválido é recusado
        mvc.perform(comToken(get("/api/anotacoes").param("aulaId", aulaId), colega))
                .andExpect(jsonPath("$[0].visibilidade").value("GRUPO"));
        mvc.perform(json(post("/api/anotacoes"), "{\"aulaId\":\"" + aulaId + "\",\"conteudo\":\"x\",\"visibilidade\":\"SECRETA\"}", autor))
                .andExpect(status().isBadRequest());
    }
}
