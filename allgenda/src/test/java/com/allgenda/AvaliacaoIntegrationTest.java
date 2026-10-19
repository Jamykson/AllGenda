package com.allgenda;

import com.allgenda.dto.request.LoginRequestDTO;
import com.allgenda.dto.request.UsuarioRequestDTO;
import com.allgenda.model.Disciplina;
import com.allgenda.model.TipoAvaliacao;
import com.allgenda.repository.AvaliacaoRepository;
import com.allgenda.repository.DisciplinaRepository;
import com.allgenda.service.AuthService;
import com.allgenda.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@Transactional
class AvaliacaoIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy filtros;
    @Autowired UsuarioService usuarios;
    @Autowired AuthService auth;
    @Autowired DisciplinaRepository disciplinas;
    @Autowired AvaliacaoRepository avaliacoes;

    private MockMvc mvc;
    private String autorizacao;
    private UUID disciplinaId;

    @BeforeEach
    void preparar() {
        mvc = webAppContextSetup(context).addFilters(filtros).build();
        usuarios.cadastrar(new UsuarioRequestDTO("Aluno", "aluno@example.com", "senha-forte-123"));
        autorizacao = "Bearer " + auth.login(new LoginRequestDTO("aluno@example.com", "senha-forte-123")).token();
        Disciplina disciplina = new Disciplina();
        disciplina.setNome("PDS");
        disciplinaId = disciplinas.saveAndFlush(disciplina).getId();
    }

    private String corpo(UUID disciplina, String descricao, String tipo) {
        return """
                {"disciplinaId":"%s","descricao":"%s","data":"2026-11-01","tipo":"%s"}
                """.formatted(disciplina, descricao, tipo);
    }

    @Test
    void cadastraProvaEEntregaEListaSomenteDisciplinaSolicitada() throws Exception {
        Disciplina outra = new Disciplina();
        outra.setNome("Outra disciplina");
        UUID outraId = disciplinas.saveAndFlush(outra).getId();
        for (TipoAvaliacao tipo : TipoAvaliacao.values()) {
            mvc.perform(post("/api/avaliacoes").header("Authorization", autorizacao)
                    .contentType("application/json").content(corpo(disciplinaId, "Avaliação de PDS", tipo.name())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.tipo").value(tipo.name()))
                    .andExpect(jsonPath("$.disciplinaId").value(disciplinaId.toString()));
        }
        mvc.perform(post("/api/avaliacoes").header("Authorization", autorizacao)
                .contentType("application/json").content(corpo(outraId, "Outra", "PROVA")))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/avaliacoes").header("Authorization", autorizacao)
                .param("disciplinaId", disciplinaId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].disciplinaNome").value("PDS"));
        assertThat(avaliacoes.findByDisciplinaIdOrderByDataAscIdAsc(disciplinaId)).hasSize(2);
    }

    @Test
    void disciplinaInexistenteRetorna404NoCadastroENaListagem() throws Exception {
        UUID inexistente = UUID.randomUUID();
        mvc.perform(post("/api/avaliacoes").header("Authorization", autorizacao)
                .contentType("application/json").content(corpo(inexistente, "Prova", "PROVA")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/avaliacoes").header("Authorization", autorizacao)
                .param("disciplinaId", inexistente.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejeitaTipoInvalidoDescricaoVaziaECamposAusentes() throws Exception {
        for (String json : new String[] {
                corpo(disciplinaId, "Prova", "INVALIDO"),
                corpo(disciplinaId, "   ", "PROVA"),
                "{}",
                corpo(disciplinaId, "Prova", "PROVA").replace("\"2026-11-01\"", "null"),
                corpo(disciplinaId, "Prova", "PROVA").replace("\"PROVA\"", "null")}) {
            mvc.perform(post("/api/avaliacoes").header("Authorization", autorizacao)
                    .contentType("application/json").content(json))
                    .andExpect(status().isBadRequest());
        }
        assertThat(avaliacoes.count()).isZero();
        mvc.perform(get("/api/avaliacoes").header("Authorization", autorizacao)
                .param("disciplinaId", disciplinaId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
