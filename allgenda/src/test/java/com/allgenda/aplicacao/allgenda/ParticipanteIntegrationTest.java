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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Lista de participantes, remoção de membro (só o dono) e edição da descrição da disciplina (só o dono).
@SpringBootTest
class ParticipanteIntegrationTest {

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
    void qualquerParticipanteVeAListaComODonoPrimeiroEAContagemSoDasAnotacoesDoGrupo() throws Exception {
        anotar("Do grupo 1", "GRUPO", membro);
        anotar("Do grupo 2", "GRUPO", membro);
        anotar("Segredo", "PRIVADA", membro); // não entra na conta

        for (Pessoa quem : new Pessoa[] {dono, membro}) {
            mvc.perform(comToken(get("/api/disciplinas/{id}/participantes", disciplinaId), quem))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].usuarioId").value(dono.id().toString()))
                    .andExpect(jsonPath("$[0].papel").value("DONO"))
                    .andExpect(jsonPath("$[0].anotacoes").value(0))
                    .andExpect(jsonPath("$[1].usuarioId").value(membro.id().toString()))
                    .andExpect(jsonPath("$[1].papel").value("MEMBRO"))
                    .andExpect(jsonPath("$[1].anotacoes").value(2));
        }
    }

    @Test
    void quemNaoParticipaNaoVeAListaEConviteNaoAceitoNaoAparece() throws Exception {
        mvc.perform(comToken(get("/api/disciplinas/{id}/participantes", disciplinaId), estranho))
                .andExpect(status().isForbidden());
        criar("/api/disciplinas/" + disciplinaId + "/convites", "{\"email\":\"" + estranho.email() + "\"}", dono); // pendente
        mvc.perform(comToken(get("/api/disciplinas/{id}/participantes", disciplinaId), dono))
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(comToken(get("/api/disciplinas/{id}/participantes", UUID.randomUUID()), dono))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/disciplinas/{id}/participantes", disciplinaId)).andExpect(status().isUnauthorized());
    }

    @Test
    void oDonoRemoveUmMembroEAnotacoesDoGrupoDeleContinuamParaOsOutros() throws Exception {
        String doGrupo = anotar("Fica no grupo", "GRUPO", membro);
        String privada = anotar("Segredo", "PRIVADA", membro);

        mvc.perform(comToken(delete("/api/disciplinas/{d}/participantes/{u}", disciplinaId, membro.id()), dono))
                .andExpect(status().isNoContent());

        // a pessoa removida perde o acesso
        mvc.perform(comToken(get("/api/disciplinas/{id}", disciplinaId), membro)).andExpect(status().isForbidden());
        mvc.perform(comToken(get("/api/disciplinas/{id}/participantes", disciplinaId), dono))
                .andExpect(jsonPath("$.length()").value(1));
        // a anotação do grupo fica; a privada continua invisível para o dono
        mvc.perform(comToken(get("/api/anotacoes").param("aulaId", aulaId), dono))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(doGrupo)))
                .andExpect(jsonPath("$[0].autorId").value(membro.id().toString()));
        mvc.perform(comToken(get("/api/anotacoes").param("aulaId", aulaId), dono))
                .andExpect(jsonPath("$[?(@.id=='" + privada + "')]").isEmpty());

        // pode ser convidado de novo e, ao aceitar, volta a ver as próprias anotações
        convidarEAceitar(membro);
        mvc.perform(comToken(get("/api/anotacoes").param("aulaId", aulaId), membro))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(doGrupo, privada)));
    }

    @Test
    void soODonoRemoveEODonoNaoPodeSerRemovido() throws Exception {
        mvc.perform(comToken(delete("/api/disciplinas/{d}/participantes/{u}", disciplinaId, membro.id()), membro))
                .andExpect(status().isForbidden());
        mvc.perform(comToken(delete("/api/disciplinas/{d}/participantes/{u}", disciplinaId, dono.id()), membro))
                .andExpect(status().isForbidden());
        mvc.perform(comToken(delete("/api/disciplinas/{d}/participantes/{u}", disciplinaId, membro.id()), estranho))
                .andExpect(status().isForbidden());
        mvc.perform(comToken(delete("/api/disciplinas/{d}/participantes/{u}", disciplinaId, dono.id()), dono))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Quem criou o grupo não pode ser removido dele."));
        mvc.perform(comToken(delete("/api/disciplinas/{d}/participantes/{u}", disciplinaId, estranho.id()), dono))
                .andExpect(status().isNotFound());
        mvc.perform(comToken(delete("/api/disciplinas/{d}/participantes/{u}", UUID.randomUUID(), membro.id()), dono))
                .andExpect(status().isNotFound());
        mvc.perform(comToken(get("/api/disciplinas/{id}/participantes", disciplinaId), dono))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void soODonoEditaADescricaoDaDisciplina() throws Exception {
        mvc.perform(json(patch("/api/disciplinas/{id}", disciplinaId), "{\"descricao\":\"  Nova descrição  \"}", dono))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Nova descrição"))
                .andExpect(jsonPath("$.nome").value("PDS"));
        mvc.perform(comToken(get("/api/disciplinas/{id}", disciplinaId), membro))
                .andExpect(jsonPath("$.descricao").value("Nova descrição"));

        mvc.perform(json(patch("/api/disciplinas/{id}", disciplinaId), "{\"descricao\":\"   \"}", dono))
                .andExpect(status().isOk()).andExpect(jsonPath("$.descricao").doesNotExist());
        mvc.perform(json(patch("/api/disciplinas/{id}", disciplinaId), "{\"descricao\":\"" + "a".repeat(501) + "\"}", dono))
                .andExpect(status().isBadRequest());
        mvc.perform(json(patch("/api/disciplinas/{id}", disciplinaId), "{\"descricao\":\"x\"}", membro))
                .andExpect(status().isForbidden());
        mvc.perform(json(patch("/api/disciplinas/{id}", UUID.randomUUID()), "{\"descricao\":\"x\"}", dono))
                .andExpect(status().isNotFound());
    }
}
