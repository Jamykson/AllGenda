package com.allgenda;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.allgenda.dto.request.LoginRequestDTO;
import com.allgenda.dto.request.UsuarioRequestDTO;
import com.allgenda.model.PapelParticipacao;
import com.allgenda.model.StatusParticipacao;
import com.allgenda.repository.ParticipacaoRepository;
import com.allgenda.service.AuthService;
import com.allgenda.service.UsuarioService;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
class ParticipacaoDonoIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy filtros;
    @Autowired AuthService auth;
    @Autowired UsuarioService cadastroUsuarios;
    @Autowired ParticipacaoRepository participacoes;

    @Test
    void criarDisciplinaTornaQuemCriouDonoEExcluirApagaAParticipacao() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filtros).build();
        String sufixo = UUID.randomUUID().toString();
        var usuario = cadastroUsuarios.cadastrar(new UsuarioRequestDTO(
                "Aluno " + sufixo, "aluno-" + sufixo + "@example.com", "senha-forte-123"));
        String token = auth.login(new LoginRequestDTO(usuario.email(), "senha-forte-123")).token();

        String resposta = mvc.perform(post("/api/disciplinas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"PDS\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID disciplinaId = UUID.fromString(JsonPath.read(resposta, "$.id"));

        var participacao = participacoes.findByUsuarioIdAndDisciplinaId(usuario.id(), disciplinaId).orElseThrow();
        assertThat(participacao.getPapel()).isEqualTo(PapelParticipacao.DONO);
        assertThat(participacao.getStatus()).isEqualTo(StatusParticipacao.ACEITO);

        mvc.perform(delete("/api/disciplinas/{id}", disciplinaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
        assertThat(participacoes.findByUsuarioIdAndDisciplinaId(usuario.id(), disciplinaId)).isEmpty();
    }
}
