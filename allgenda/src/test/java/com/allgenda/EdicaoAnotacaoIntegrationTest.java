package com.allgenda;

import com.allgenda.dto.request.LoginRequestDTO;
import com.allgenda.dto.request.UsuarioRequestDTO;
import com.allgenda.repository.AnotacaoRepository;
import com.allgenda.service.AuthService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class EdicaoAnotacaoIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy filtros;
    @Autowired AuthService auth;
    @Autowired AnotacaoRepository anotacoes;
    @Autowired TransactionTemplate transacao;

    private MockMvc mvc;
    private String tokenAutor;
    private String tokenColega;
    private String autorId;
    private String aulaId;
    private String anotacaoId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filtros).build();

        String sufixo = UUID.randomUUID().toString();
        var autor = auth.cadastrar(new UsuarioRequestDTO("Autor " + sufixo, "autor-" + sufixo + "@example.com", "senha-forte-123"));
        var colega = auth.cadastrar(new UsuarioRequestDTO("Colega " + sufixo, "colega-" + sufixo + "@example.com", "senha-forte-123"));
        autorId = autor.id().toString();
        tokenAutor = auth.login(new LoginRequestDTO(autor.email(), "senha-forte-123")).token();
        tokenColega = auth.login(new LoginRequestDTO(colega.email(), "senha-forte-123")).token();

        String disciplinaId = criar("/api/disciplinas", "{\"nome\":\"PDS\"}");
        String conviteId = criar("/api/disciplinas/" + disciplinaId + "/convites", "{\"email\":\"" + colega.email() + "\"}");
        mvc.perform(comToken(patch("/api/convites/{id}/aceitar", conviteId), tokenColega)).andExpect(status().isOk());
        aulaId = criar("/api/aulas", "{\"disciplinaId\":\"" + disciplinaId + "\",\"data\":\"2026-10-19\",\"horario\":\"08:00\",\"topico\":\"Padrões de projeto\"}");
        anotacaoId = criar("/api/anotacoes", "{\"aulaId\":\"" + aulaId + "\",\"conteudo\":\"Texto original\",\"tags\":[\"java\"]}");
    }

    private static RequestBuilder comToken(AbstractMockHttpServletRequestBuilder<?> requisicao, String token) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String criar(String caminho, String json) throws Exception {
        String resposta = mvc.perform(comToken(post(caminho).contentType(MediaType.APPLICATION_JSON).content(json), tokenAutor))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private RequestBuilder edicao(String token, String conteudo) {
        return comToken(patch("/api/anotacoes/{id}", anotacaoId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"conteudo\":\"" + conteudo + "\"}"), token);
    }

    private RequestBuilder novasTags(String token) {
        return comToken(post("/api/anotacoes/{id}/tags", anotacaoId)
                .contentType(MediaType.APPLICATION_JSON).content("[\"spring\"]"), token);
    }

    private String conteudoSalvo() {
        return anotacoes.findById(UUID.fromString(anotacaoId)).orElseThrow().getConteudo();
    }

    private int quantidadeDeTagsSalvas() {
        // as tags são carregadas sob demanda, então a leitura precisa de uma transação
        return transacao.execute(status -> anotacoes.findById(UUID.fromString(anotacaoId)).orElseThrow().getTags().size());
    }

    @Test
    void autorEditaConteudoERespostaTrazOIdDoAutor() throws Exception {
        mvc.perform(edicao(tokenAutor, "Texto revisado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").value("Texto revisado"))
                .andExpect(jsonPath("$.autorId").value(autorId));

        assertThat(conteudoSalvo()).isEqualTo("Texto revisado");
    }

    @Test
    void colegaNaoEditaAnotacaoDoAutor() throws Exception {
        mvc.perform(edicao(tokenColega, "Texto do colega"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Somente o autor pode editar a anotação."));

        assertThat(conteudoSalvo()).isEqualTo("Texto original");
    }

    @Test
    void autorAcrescentaTags() throws Exception {
        mvc.perform(novasTags(tokenAutor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags.length()").value(2));
    }

    @Test
    void colegaTambemAcrescentaTagsNaAnotacaoDoAutor() throws Exception {
        mvc.perform(novasTags(tokenColega))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags.length()").value(2))
                .andExpect(jsonPath("$.autorId").value(autorId)); // o autor continua sendo quem criou a anotação

        assertThat(quantidadeDeTagsSalvas()).isEqualTo(2);
    }

    @Test
    void listagemTrazOIdDoAutorDeCadaAnotacao() throws Exception {
        mvc.perform(comToken(get("/api/anotacoes").param("aulaId", aulaId), tokenColega))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].autorId").value(autorId));
    }
}
