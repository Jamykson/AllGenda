package com.allgenda;

import com.allgenda.dto.request.LoginRequestDTO;
import com.allgenda.dto.request.UsuarioRequestDTO;
import com.allgenda.service.AuthService;
import com.allgenda.service.UsuarioService;
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
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Tags são da disciplina: o mesmo nome em duas disciplinas são duas tags, e renomear ou excluir
// uma não afeta a outra. Qualquer participante gerencia as tags da disciplina.
@SpringBootTest
class TagIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy filtros;
    @Autowired AuthService auth;
    @Autowired UsuarioService cadastroUsuarios;

    private MockMvc mvc;
    private String tokenAutor;
    private String tokenColega;
    private String tokenDeFora;
    private String disciplinaA;
    private String disciplinaB;
    private String aulaA;
    private String aulaB;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filtros).build();

        String sufixo = UUID.randomUUID().toString();
        tokenAutor = cadastrarELogar("Autor", sufixo);
        tokenColega = cadastrarELogar("Colega", sufixo);
        tokenDeFora = cadastrarELogar("Visitante", sufixo);

        disciplinaA = criar("/api/disciplinas", "{\"nome\":\"PDS\"}", tokenAutor);
        disciplinaB = criar("/api/disciplinas", "{\"nome\":\"Grafos\"}", tokenAutor);
        // o colega participa só da disciplina A
        String conviteId = criar("/api/disciplinas/" + disciplinaA + "/convites", "{\"email\":\"colega-" + sufixo + "@example.com\"}", tokenAutor);
        mvc.perform(comToken(patch("/api/convites/{id}/aceitar", conviteId), tokenColega)).andExpect(status().isOk());

        aulaA = criarAula(disciplinaA);
        aulaB = criarAula(disciplinaB);
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

    private String criarAula(String disciplinaId) throws Exception {
        return criar("/api/aulas", "{\"disciplinaId\":\"" + disciplinaId
                + "\",\"data\":\"2026-10-20\",\"horario\":\"08:00\",\"topico\":\"Aula\"}", tokenAutor);
    }

    private String criarAnotacao(String aulaId, String tagsJson) throws Exception {
        return criar("/api/anotacoes", "{\"aulaId\":\"" + aulaId + "\",\"conteudo\":\"Texto\",\"tags\":" + tagsJson + "}", tokenAutor);
    }

    // id da tag com esse nome na disciplina, pela listagem da API
    private String idDaTag(String disciplinaId, String nome) throws Exception {
        String resposta = mvc.perform(comToken(get("/api/tags").param("disciplinaId", disciplinaId), tokenAutor))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(resposta, "$[?(@.nome == '" + nome + "')].id");
        assertThat(ids).hasSize(1);
        return ids.getFirst();
    }

    private RequestBuilder renomeacao(String tagId, String nome, String token) {
        return comToken(patch("/api/tags/{id}", tagId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"" + nome + "\"}"), token);
    }

    @Test
    void mesmoNomeEmDuasDisciplinasSaoDuasTags() throws Exception {
        criarAnotacao(aulaA, "[\"Prova\"]");
        criarAnotacao(aulaB, "[\"prova\"]");

        String tagA = idDaTag(disciplinaA, "Prova");
        String tagB = idDaTag(disciplinaB, "prova"); // cada disciplina guarda a grafia de quando criou a tag
        assertThat(tagA).isNotEqualTo(tagB);
        mvc.perform(comToken(get("/api/tags").param("disciplinaId", disciplinaA), tokenAutor))
                .andExpect(jsonPath("$[0].disciplinaId").value(disciplinaA));
    }

    @Test
    void listagemPorDisciplinaVemEmOrdemAlfabeticaEExigeParticipacao() throws Exception {
        criarAnotacao(aulaA, "[\"zeta\",\"Árvore\",\"banco\"]");
        criarAnotacao(aulaB, "[\"grafo\"]");

        mvc.perform(comToken(get("/api/tags").param("disciplinaId", disciplinaA), tokenColega))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", contains("Árvore", "banco", "zeta")));
        // sem filtro, só as tags das disciplinas em que o usuário participa
        mvc.perform(comToken(get("/api/tags"), tokenColega))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", contains("Árvore", "banco", "zeta")));

        mvc.perform(comToken(get("/api/tags").param("disciplinaId", disciplinaB), tokenColega))
                .andExpect(status().isForbidden());
        mvc.perform(comToken(get("/api/tags"), tokenDeFora))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void renomearMudaONomeNasAnotacoesSoDaquelaDisciplina() throws Exception {
        criarAnotacao(aulaA, "[\"Prova\"]");
        criarAnotacao(aulaA, "[\"Prova\",\"Resumo\"]");
        criarAnotacao(aulaB, "[\"Prova\"]");

        // o colega não é autor das anotações, mas participa da disciplina
        mvc.perform(renomeacao(idDaTag(disciplinaA, "Prova"), "Prova 1", tokenColega))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Prova 1"));

        mvc.perform(comToken(get("/api/anotacoes").param("aulaId", aulaA), tokenAutor))
                .andExpect(jsonPath("$[0].tags", contains("Prova 1")))
                .andExpect(jsonPath("$[1].tags[?(@ == 'Prova 1')]").exists());
        mvc.perform(comToken(get("/api/anotacoes").param("aulaId", aulaB), tokenAutor))
                .andExpect(jsonPath("$[0].tags", contains("Prova")));
    }

    @Test
    void renomearParaUmNomeQueJaExisteNaDisciplinaDaConflito() throws Exception {
        criarAnotacao(aulaA, "[\"Prova\",\"Resumo\"]");
        String prova = idDaTag(disciplinaA, "Prova");

        mvc.perform(renomeacao(prova, "resumo", tokenAutor))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe uma tag com esse nome nesta disciplina."));
        // mudar só maiúsculas e minúsculas da própria tag é permitido
        mvc.perform(renomeacao(prova, "PROVA", tokenAutor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("PROVA"));
        mvc.perform(renomeacao(prova, "  ", tokenAutor))
                .andExpect(status().isBadRequest());
    }

    @Test
    void excluirTiraATagDasAnotacoesSoDaquelaDisciplina() throws Exception {
        criarAnotacao(aulaA, "[\"Prova\",\"Resumo\"]");
        criarAnotacao(aulaB, "[\"Prova\"]");
        String prova = idDaTag(disciplinaA, "Prova");

        mvc.perform(comToken(delete("/api/tags/{id}", prova), tokenColega))
                .andExpect(status().isNoContent());

        mvc.perform(comToken(get("/api/anotacoes").param("aulaId", aulaA), tokenAutor))
                .andExpect(jsonPath("$[0].tags", contains("Resumo")));
        mvc.perform(comToken(get("/api/tags").param("disciplinaId", disciplinaA), tokenAutor))
                .andExpect(jsonPath("$[*].nome", contains("Resumo")));
        mvc.perform(comToken(get("/api/anotacoes").param("aulaId", aulaB), tokenAutor))
                .andExpect(jsonPath("$[0].tags", contains("Prova")));
        mvc.perform(comToken(delete("/api/tags/{id}", prova), tokenAutor))
                .andExpect(status().isNotFound());
    }

    @Test
    void quemNaoParticipaNaoRenomeiaNemExclui() throws Exception {
        criarAnotacao(aulaA, "[\"Prova\"]");
        String prova = idDaTag(disciplinaA, "Prova");

        mvc.perform(renomeacao(prova, "Outra", tokenDeFora)).andExpect(status().isForbidden());
        mvc.perform(comToken(delete("/api/tags/{id}", prova), tokenDeFora)).andExpect(status().isForbidden());

        assertThat(idDaTag(disciplinaA, "Prova")).isEqualTo(prova);
    }
}
