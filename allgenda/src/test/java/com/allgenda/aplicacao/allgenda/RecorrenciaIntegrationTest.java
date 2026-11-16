package com.allgenda.aplicacao.allgenda;

import com.allgenda.framework.dto.request.LoginRequestDTO;
import com.allgenda.framework.dto.request.UsuarioRequestDTO;
import com.allgenda.framework.service.AuthService;
import com.allgenda.framework.service.UsuarioService;
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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Recorrência da disciplina: no máximo uma por disciplina, só o dono define, qualquer participante vê.
@SpringBootTest
class RecorrenciaIntegrationTest {

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

    private static final String CORPO = "{\"diasSemana\":[\"QUA\",\"SEG\"],\"horaInicio\":\"08:00\",\"horaFim\":\"09:40\","
            + "\"dataInicio\":\"2026-08-03\",\"dataFim\":\"2026-12-18\",\"frequencia\":\"QUINZENAL\"}";

    private String url() {
        return "/api/disciplinas/" + disciplinaId + "/recorrencia";
    }

    @Test
    void semRecorrenciaDa404() throws Exception {
        mvc.perform(comToken(get(url()), dono)).andExpect(status().isNotFound());
        mvc.perform(comToken(get("/api/recorrencias"), dono))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void oDonoDefineAsDatasSaemOrdenadasEQualquerParticipanteVe() throws Exception {
        mvc.perform(json(put(url()), CORPO, dono))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disciplinaId").value(disciplinaId))
                .andExpect(jsonPath("$.diasSemana", contains("SEG", "QUA")))
                .andExpect(jsonPath("$.horaInicio").value("08:00"))
                .andExpect(jsonPath("$.horaFim").value("09:40"))
                .andExpect(jsonPath("$.dataInicio").value("2026-08-03"))
                .andExpect(jsonPath("$.dataFim").value("2026-12-18"))
                .andExpect(jsonPath("$.frequencia").value("QUINZENAL"));

        for (Pessoa quem : new Pessoa[] {dono, membro}) {
            mvc.perform(comToken(get(url()), quem))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.frequencia").value("QUINZENAL"));
            mvc.perform(comToken(get("/api/recorrencias"), quem))
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].disciplinaId").value(disciplinaId));
        }
        // quem não participa não vê a recorrência nem na lista geral
        mvc.perform(comToken(get(url()), estranho)).andExpect(status().isForbidden());
        mvc.perform(comToken(get("/api/recorrencias"), estranho))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void salvarDeNovoSubstituiEFrequenciaAusenteVaiParaSemanal() throws Exception {
        mvc.perform(json(put(url()), CORPO, dono)).andExpect(status().isOk());
        mvc.perform(json(put(url()), "{\"diasSemana\":[\"SEX\"],\"horaInicio\":\"14:00\",\"horaFim\":\"15:40\","
                + "\"dataInicio\":\"2026-09-01\",\"dataFim\":\"2026-09-30\"}", dono))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diasSemana", contains("SEX")))
                .andExpect(jsonPath("$.frequencia").value("SEMANAL"));
        mvc.perform(comToken(get("/api/recorrencias"), dono)).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void dadosInvalidosDao400() throws Exception {
        String[] ruins = {
            "{\"diasSemana\":[],\"horaInicio\":\"08:00\",\"horaFim\":\"09:00\",\"dataInicio\":\"2026-08-03\",\"dataFim\":\"2026-12-18\"}",
            "{\"horaInicio\":\"08:00\",\"horaFim\":\"09:00\",\"dataInicio\":\"2026-08-03\",\"dataFim\":\"2026-12-18\"}",
            "{\"diasSemana\":[\"SEG\"],\"horaInicio\":\"8h\",\"horaFim\":\"09:00\",\"dataInicio\":\"2026-08-03\",\"dataFim\":\"2026-12-18\"}",
            "{\"diasSemana\":[\"SEG\"],\"horaInicio\":\"09:00\",\"horaFim\":\"09:00\",\"dataInicio\":\"2026-08-03\",\"dataFim\":\"2026-12-18\"}",
            "{\"diasSemana\":[\"SEG\"],\"horaInicio\":\"08:00\",\"horaFim\":\"09:00\",\"dataInicio\":\"2026-12-18\",\"dataFim\":\"2026-08-03\"}",
            "{\"diasSemana\":[\"SEG\"],\"horaInicio\":\"08:00\",\"horaFim\":\"09:00\"}",
            "{\"diasSemana\":[\"XYZ\"],\"horaInicio\":\"08:00\",\"horaFim\":\"09:00\",\"dataInicio\":\"2026-08-03\",\"dataFim\":\"2026-12-18\"}"
        };
        for (String corpo : ruins) {
            mvc.perform(json(put(url()), corpo, dono)).andExpect(status().isBadRequest());
        }
        mvc.perform(comToken(get(url()), dono)).andExpect(status().isNotFound());
    }

    @Test
    void soODonoDefineERemove() throws Exception {
        mvc.perform(json(put(url()), CORPO, membro)).andExpect(status().isForbidden());
        mvc.perform(json(put(url()), CORPO, estranho)).andExpect(status().isForbidden());
        mvc.perform(put(url()).contentType(MediaType.APPLICATION_JSON).content(CORPO)).andExpect(status().isUnauthorized());
        mvc.perform(json(put("/api/disciplinas/{id}/recorrencia", UUID.randomUUID()), CORPO, dono)).andExpect(status().isNotFound());

        mvc.perform(json(put(url()), CORPO, dono)).andExpect(status().isOk());
        mvc.perform(comToken(delete(url()), membro)).andExpect(status().isForbidden());
        mvc.perform(comToken(delete(url()), estranho)).andExpect(status().isForbidden());
        mvc.perform(comToken(get(url()), dono)).andExpect(status().isOk());
    }

    @Test
    void removerApagaARecorrenciaMasMantemAsAulasJaCadastradas() throws Exception {
        mvc.perform(json(put(url()), CORPO, dono)).andExpect(status().isOk());
        mvc.perform(comToken(delete(url()), dono)).andExpect(status().isNoContent());
        mvc.perform(comToken(get(url()), dono)).andExpect(status().isNotFound());
        mvc.perform(comToken(delete(url()), dono)).andExpect(status().isNotFound());
        mvc.perform(comToken(get("/api/aulas/{id}", aulaId), dono)).andExpect(status().isOk());
    }

    @Test
    void excluirADisciplinaApagaASuaRecorrencia() throws Exception {
        mvc.perform(json(put(url()), CORPO, dono)).andExpect(status().isOk());
        mvc.perform(comToken(delete("/api/disciplinas/{id}", disciplinaId), dono)).andExpect(status().isNoContent());
        mvc.perform(comToken(get("/api/recorrencias"), dono)).andExpect(jsonPath("$.length()").value(0));
    }
}
