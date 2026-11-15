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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Editar, cancelar e excluir aula: só o dono; cancelar mantém a aula e as anotações; excluir apaga tudo da aula.
@SpringBootTest
class EdicaoAulaIntegrationTest {

    private record Pessoa(UUID id, String email, String token) {}

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy filtros;
    @Autowired AuthService auth;
    @Autowired UsuarioService cadastroUsuarios;

    private MockMvc mvc;
    private Pessoa dono;
    private Pessoa membro;
    private Pessoa estranho;
    private String disciplinaId;
    private String aulaId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filtros).build();
        dono = criarPessoa("Dono");
        membro = criarPessoa("Membro");
        estranho = criarPessoa("Estranho");

        disciplinaId = criar("/api/disciplinas", "{\"nome\":\"PDS\",\"descricao\":\"Turma de segunda\"}", dono);
        convidarEAceitar(membro);
        aulaId = criar("/api/aulas", "{\"disciplinaId\":\"" + disciplinaId
                + "\",\"data\":\"2026-10-19\",\"horario\":\"08:00\",\"topico\":\"Strategy\"}", dono);
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

    private RequestBuilder json(AbstractMockHttpServletRequestBuilder<?> requisicao, String corpo, Pessoa quem) {
        return comToken(requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo), quem);
    }

    private String criar(String caminho, String corpo, Pessoa quem) throws Exception {
        String resposta = mvc.perform(json(post(caminho), corpo, quem))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    private void convidarEAceitar(Pessoa convidado) throws Exception {
        String conviteId = criar("/api/disciplinas/" + disciplinaId + "/convites", "{\"email\":\"" + convidado.email() + "\"}", dono);
        mvc.perform(comToken(patch("/api/convites/{id}/aceitar", conviteId), convidado)).andExpect(status().isOk());
    }

    private String anotar(String conteudo, String visibilidade, Pessoa quem) throws Exception {
        return criar("/api/anotacoes", "{\"aulaId\":\"" + aulaId + "\",\"conteudo\":\"" + conteudo
                + "\",\"visibilidade\":\"" + visibilidade + "\"}", quem);
    }

    @Test
    void oDonoEditaDataHorarioETopicoEOQueNaoVemNaoMuda() throws Exception {
        mvc.perform(json(patch("/api/aulas/{id}", aulaId), "{\"topico\":\"  Observer  \",\"horario\":\"10:00\"}", dono))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topico").value("Observer"))
                .andExpect(jsonPath("$.horario").value("10:00"))
                .andExpect(jsonPath("$.data").value("2026-10-19"))
                .andExpect(jsonPath("$.cancelada").value(false));
        mvc.perform(json(patch("/api/aulas/{id}", aulaId), "{\"data\":\"2026-10-26\"}", dono))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("2026-10-26"))
                .andExpect(jsonPath("$.topico").value("Observer"));
        mvc.perform(comToken(get("/api/aulas/{id}", aulaId), membro))
                .andExpect(jsonPath("$.topico").value("Observer"));
    }

    @Test
    void camposEmBrancoDaoErro400() throws Exception {
        mvc.perform(json(patch("/api/aulas/{id}", aulaId), "{\"topico\":\"   \"}", dono)).andExpect(status().isBadRequest());
        mvc.perform(json(patch("/api/aulas/{id}", aulaId), "{\"horario\":\"\"}", dono)).andExpect(status().isBadRequest());
        mvc.perform(comToken(get("/api/aulas/{id}", aulaId), dono)).andExpect(jsonPath("$.topico").value("Strategy"));
    }

    @Test
    void cancelarMantemAAulaEAsAnotacoesEPodeSerDesfeito() throws Exception {
        String nota = anotar("Continua aqui", "GRUPO", membro);

        mvc.perform(json(patch("/api/aulas/{id}", aulaId), "{\"cancelada\":true}", dono))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cancelada").value(true));

        mvc.perform(comToken(get("/api/aulas").param("disciplinaId", disciplinaId), membro))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].cancelada").value(true))
                .andExpect(jsonPath("$[0].anotacoes").value(1));
        mvc.perform(comToken(get("/api/anotacoes").param("aulaId", aulaId), dono))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(nota)));

        mvc.perform(json(patch("/api/aulas/{id}", aulaId), "{\"cancelada\":false}", dono))
                .andExpect(jsonPath("$.cancelada").value(false));
    }

    @Test
    void aContagemDeAnotacoesNaoIncluiAsPrivadas() throws Exception {
        anotar("Do grupo", "GRUPO", membro);
        anotar("Segredo", "PRIVADA", membro);
        mvc.perform(comToken(get("/api/aulas/{id}", aulaId), dono)).andExpect(jsonPath("$.anotacoes").value(1));
    }

    @Test
    void soODonoEditaCancelaEExclui() throws Exception {
        mvc.perform(json(patch("/api/aulas/{id}", aulaId), "{\"topico\":\"X\"}", membro)).andExpect(status().isForbidden());
        mvc.perform(json(patch("/api/aulas/{id}", aulaId), "{\"cancelada\":true}", membro)).andExpect(status().isForbidden());
        mvc.perform(json(patch("/api/aulas/{id}", aulaId), "{\"topico\":\"X\"}", estranho)).andExpect(status().isForbidden());
        mvc.perform(comToken(delete("/api/aulas/{id}", aulaId), membro)).andExpect(status().isForbidden());
        mvc.perform(comToken(delete("/api/aulas/{id}", aulaId), estranho)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/aulas/{id}", aulaId).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/aulas/{id}", aulaId)).andExpect(status().isUnauthorized());
        // nada mudou
        mvc.perform(comToken(get("/api/aulas/{id}", aulaId), dono))
                .andExpect(status().isOk()).andExpect(jsonPath("$.topico").value("Strategy"));
    }

    @Test
    void oDonoExcluiAAulaEAsAnotacoesSaemJuntoMasAsOutrasAulasFicam() throws Exception {
        String outraAula = criar("/api/aulas", "{\"disciplinaId\":\"" + disciplinaId
                + "\",\"data\":\"2026-10-26\",\"horario\":\"08:00\",\"topico\":\"Observer\"}", dono);
        anotar("Some junto", "GRUPO", membro);
        anotar("Segredo some junto", "PRIVADA", membro);

        mvc.perform(comToken(delete("/api/aulas/{id}", aulaId), dono)).andExpect(status().isNoContent());

        mvc.perform(comToken(get("/api/aulas/{id}", aulaId), dono)).andExpect(status().isNotFound());
        mvc.perform(comToken(get("/api/aulas").param("disciplinaId", disciplinaId), dono))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(outraAula));
        mvc.perform(comToken(delete("/api/aulas/{id}", aulaId), dono)).andExpect(status().isNotFound());
        mvc.perform(json(patch("/api/aulas/{id}", UUID.randomUUID()), "{\"topico\":\"X\"}", dono)).andExpect(status().isNotFound());
    }
}
