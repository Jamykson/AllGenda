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

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sem @Transactional, como os outros testes de integração: cada requisição confirma a própria transação.
@SpringBootTest
class DisciplinasDoUsuarioIntegrationTest {

    private record Pessoa(String email, String token) {}

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy filtros;
    @Autowired UsuarioService cadastroUsuarios;
    @Autowired AuthService auth;

    private MockMvc mvc;
    private Pessoa dono;
    private Pessoa convidado;
    private String disciplinaId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filtros).build();
        dono = criarPessoa("Dono");
        convidado = criarPessoa("Convidado");
        disciplinaId = criar("/api/disciplinas", "{\"nome\":\"PDS\"}", dono);
    }

    private Pessoa criarPessoa(String prefixo) {
        String sufixo = UUID.randomUUID().toString();
        String email = prefixo.toLowerCase() + "-" + sufixo + "@example.com";
        cadastroUsuarios.cadastrar(new UsuarioRequestDTO(prefixo + " " + sufixo, email, "senha-forte-123"));
        return new Pessoa(email, auth.login(new LoginRequestDTO(email, "senha-forte-123")).token());
    }

    private RequestBuilder comToken(AbstractMockHttpServletRequestBuilder<?> requisicao, Pessoa quem) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + quem.token());
    }

    private String criar(String caminho, String json, Pessoa quem) throws Exception {
        String resposta = mvc.perform(comToken(post(caminho)
                        .contentType(MediaType.APPLICATION_JSON).content(json), quem))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private String convidar() throws Exception {
        return criar("/api/disciplinas/" + disciplinaId + "/convites",
                "{\"email\":\"" + convidado.email() + "\"}", dono);
    }

    @Test
    void donoVeASuaDisciplina() throws Exception {
        mvc.perform(comToken(get("/api/disciplinas"), dono))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(disciplinaId)));
    }

    @Test
    void quemNaoParticipaNaoVeADisciplina() throws Exception {
        mvc.perform(comToken(get("/api/disciplinas"), convidado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    void convitePendenteOuRecusadoNaoMostraADisciplina() throws Exception {
        String conviteId = convidar();   // pendente
        mvc.perform(comToken(get("/api/disciplinas"), convidado))
                .andExpect(jsonPath("$", empty()));

        mvc.perform(comToken(patch("/api/convites/{id}/recusar", conviteId), convidado)).andExpect(status().isOk());
        mvc.perform(comToken(get("/api/disciplinas"), convidado))
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    void aposAceitarOConviteADisciplinaAparece() throws Exception {
        String conviteId = convidar();
        mvc.perform(comToken(patch("/api/convites/{id}/aceitar", conviteId), convidado)).andExpect(status().isOk());

        mvc.perform(comToken(get("/api/disciplinas"), convidado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(disciplinaId)));
    }
}