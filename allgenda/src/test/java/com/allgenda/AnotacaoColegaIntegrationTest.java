package com.allgenda;

import java.util.UUID;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.allgenda.dto.request.LoginRequestDTO;
import com.allgenda.dto.request.UsuarioRequestDTO;
import com.allgenda.service.AuthService;
import com.jayway.jsonpath.JsonPath;

// Sem @Transactional, como os outros testes de integração: cada requisição confirma a própria transação.
@SpringBootTest
class AnotacaoColegaIntegrationTest {

    private record Pessoa(UUID id, String email, String token) {}

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy filtros;
    @Autowired AuthService auth;

    private MockMvc mvc;
    private Pessoa dono;
    private Pessoa colega;
    private Pessoa estranho;
    private String disciplinaId;
    private String anotacaoDoDono;
    private String anotacaoDoColega;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filtros).build();
        dono = criarPessoa("Dono");
        colega = criarPessoa("Colega");
        estranho = criarPessoa("Estranho");

        disciplinaId = criar("/api/disciplinas", "{\"nome\":\"PDS\"}", dono);
        String aulaId = criar("/api/aulas", "{\"disciplinaId\":\"" + disciplinaId
                + "\",\"data\":\"2026-10-19\",\"horario\":\"08:00\",\"topico\":\"Strategy\"}", dono);
        anotacaoDoDono = criar("/api/anotacoes", "{\"aulaId\":\"" + aulaId + "\",\"conteudo\":\"Do dono\",\"tags\":[\"java\"]}", dono);
        anotacaoDoColega = criar("/api/anotacoes", "{\"aulaId\":\"" + aulaId + "\",\"conteudo\":\"Do colega\",\"tags\":[]}", colega);
    }

    private Pessoa criarPessoa(String prefixo) {
        String sufixo = UUID.randomUUID().toString();
        String email = prefixo.toLowerCase() + "-" + sufixo + "@example.com";
        var usuario = auth.cadastrar(new UsuarioRequestDTO(prefixo + " " + sufixo, email, "senha-forte-123"));
        String token = auth.login(new LoginRequestDTO(email, "senha-forte-123")).token();
        return new Pessoa(usuario.id(), email, token);
    }

    private RequestBuilder comToken(AbstractMockHttpServletRequestBuilder<?> requisicao, Pessoa quem) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + quem.token());
    }

    // faz um POST que deve criar algo e devolve o "id" da resposta
    private String criar(String caminho, String json, Pessoa quem) throws Exception {
        String resposta = mvc.perform(comToken(post(caminho)
                        .contentType(MediaType.APPLICATION_JSON).content(json), quem))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    // o dono convida a pessoa e devolve o id do convite
    private String convidar(Pessoa convidado) throws Exception {
        return criar("/api/disciplinas/" + disciplinaId + "/convites",
                "{\"email\":\"" + convidado.email() + "\"}", dono);
    }

    @Test
    void participanteAceitoVeSomenteAsAnotacoesDosOutros() throws Exception {
        String conviteId = convidar(colega);
        mvc.perform(comToken(patch("/api/convites/{id}/aceitar", conviteId), colega)).andExpect(status().isOk());

        mvc.perform(comToken(get("/api/disciplinas/{id}/anotacoes/colegas", disciplinaId), colega))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(anotacaoDoDono))
                .andExpect(jsonPath("$[0].autorId").value(dono.id().toString()));

        mvc.perform(comToken(get("/api/disciplinas/{id}/anotacoes/colegas", disciplinaId), dono))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(anotacaoDoColega));
    }

    @Test
    void semParticipacaoAceitaNaoVeAsAnotacoes() throws Exception {
        // nunca convidado
        mvc.perform(comToken(get("/api/disciplinas/{id}/anotacoes/colegas", disciplinaId), estranho))
                .andExpect(status().isForbidden());

        // convite ainda pendente
        String conviteId = convidar(estranho);
        mvc.perform(comToken(get("/api/disciplinas/{id}/anotacoes/colegas", disciplinaId), estranho))
                .andExpect(status().isForbidden());

        // convite recusado
        mvc.perform(comToken(patch("/api/convites/{id}/recusar", conviteId), estranho)).andExpect(status().isOk());
        mvc.perform(comToken(get("/api/disciplinas/{id}/anotacoes/colegas", disciplinaId), estranho))
                .andExpect(status().isForbidden());
    }

    @Test
    void disciplinaInexistenteRecebe404ESemTokenRecebe401() throws Exception {
        mvc.perform(comToken(get("/api/disciplinas/{id}/anotacoes/colegas", UUID.randomUUID()), dono))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/disciplinas/{id}/anotacoes/colegas", disciplinaId))
                .andExpect(status().isUnauthorized());
    }
}