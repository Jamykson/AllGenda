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
import org.springframework.test.web.servlet.ResultActions;
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
import com.allgenda.service.UsuarioService;
import com.jayway.jsonpath.JsonPath;

// Sem @Transactional, como os outros testes de integração: cada requisição confirma a própria transação.
@SpringBootTest
class ConviteIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy filtros;
    @Autowired AuthService auth;
    @Autowired UsuarioService cadastroUsuarios;

    private MockMvc mvc;
    private String emailDono;
    private String emailColega;
    private String tokenDono;
    private String tokenColega;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filtros).build();

        String sufixo = UUID.randomUUID().toString();
        emailDono = "dono-" + sufixo + "@example.com";
        emailColega = "colega-" + sufixo + "@example.com";
        cadastroUsuarios.cadastrar(new UsuarioRequestDTO("Dono " + sufixo, emailDono, "senha-forte-123"));
        cadastroUsuarios.cadastrar(new UsuarioRequestDTO("Colega " + sufixo, emailColega, "senha-forte-123"));
        tokenDono = auth.login(new LoginRequestDTO(emailDono, "senha-forte-123")).token();
        tokenColega = auth.login(new LoginRequestDTO(emailColega, "senha-forte-123")).token();
    }

    private RequestBuilder comToken(AbstractMockHttpServletRequestBuilder<?> requisicao, String token) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String criarDisciplina() throws Exception {
        String resposta = mvc.perform(comToken(post("/api/disciplinas")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"PDS\"}"), tokenDono))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private ResultActions convidar(String disciplinaId, String email, String token) throws Exception {
        return mvc.perform(comToken(post("/api/disciplinas/{id}/convites", disciplinaId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\"}"), token));
    }

    // o dono convida o colega e devolve o id do convite
    private String convidarColega(String disciplinaId) throws Exception {
        String resposta = convidar(disciplinaId, emailColega, tokenDono)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    @Test
    void donoConvidaColegaQueAceitaEOConviteMudaDeLista() throws Exception {
        String disciplinaId = criarDisciplina();

        String resposta = convidar(disciplinaId, emailColega, tokenDono)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.papel").value("MEMBRO"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.disciplinaId").value(disciplinaId))
                .andReturn().getResponse().getContentAsString();
        String conviteId = JsonPath.read(resposta, "$.id");

        mvc.perform(comToken(get("/api/convites"), tokenColega))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(conviteId));

        mvc.perform(comToken(patch("/api/convites/{id}/aceitar", conviteId), tokenColega))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACEITO"));

        mvc.perform(comToken(get("/api/convites"), tokenColega))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(comToken(get("/api/convites").param("status", "ACEITO"), tokenColega))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void conviteRecusadoNaoPodeSerRespondidoNemReenviado() throws Exception {
        String disciplinaId = criarDisciplina();
        String conviteId = convidarColega(disciplinaId);

        mvc.perform(comToken(patch("/api/convites/{id}/recusar", conviteId), tokenColega))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECUSADO"));

        mvc.perform(comToken(patch("/api/convites/{id}/aceitar", conviteId), tokenColega))
                .andExpect(status().isConflict());
        convidar(disciplinaId, emailColega, tokenDono).andExpect(status().isConflict());
    }

    @Test
    void apenasODonoConvida_MembroAceitoNaoConvida() throws Exception {
        String disciplinaId = criarDisciplina();

        // quem não participa da disciplina
        convidar(disciplinaId, emailDono, tokenColega).andExpect(status().isForbidden());

        // quem é membro aceito também não
        String conviteId = convidarColega(disciplinaId);
        mvc.perform(comToken(patch("/api/convites/{id}/aceitar", conviteId), tokenColega))
                .andExpect(status().isOk());
        convidar(disciplinaId, emailDono, tokenColega).andExpect(status().isForbidden());
    }

    @Test
    void errosDeEnvio() throws Exception {
        String disciplinaId = criarDisciplina();

        convidar(disciplinaId, "isto-nao-e-email", tokenDono).andExpect(status().isBadRequest());
        convidar(disciplinaId, "ninguem-" + UUID.randomUUID() + "@example.com", tokenDono).andExpect(status().isNotFound());
        convidar(UUID.randomUUID().toString(), emailColega, tokenDono).andExpect(status().isNotFound());
        convidar(disciplinaId, emailDono, tokenDono).andExpect(status().isConflict()); // autoconvite

        convidarColega(disciplinaId);
        convidar(disciplinaId, emailColega, tokenDono).andExpect(status().isConflict()); // convite duplicado
    }

    @Test
    void soODestinatarioRespondeAoConvite() throws Exception {
        String conviteId = convidarColega(criarDisciplina());

        mvc.perform(comToken(patch("/api/convites/{id}/aceitar", conviteId), tokenDono))
                .andExpect(status().isForbidden());
        mvc.perform(comToken(patch("/api/convites/{id}/aceitar", UUID.randomUUID()), tokenColega))
                .andExpect(status().isNotFound());
    }

    @Test
    void statusInvalidoRecebe400ESemTokenRecebe401() throws Exception {
        mvc.perform(comToken(get("/api/convites").param("status", "QUALQUER"), tokenColega))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/convites")).andExpect(status().isUnauthorized());
    }
}