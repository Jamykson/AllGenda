package com.allgenda;

import com.allgenda.dto.request.LoginRequestDTO;
import com.allgenda.dto.request.UsuarioRequestDTO;
import com.allgenda.dto.response.UsuarioResponseDTO;
import com.allgenda.model.Disciplina;
import com.allgenda.model.PapelParticipacao;
import com.allgenda.model.Participacao;
import com.allgenda.model.StatusParticipacao;
import com.allgenda.model.TipoAvaliacao;
import com.allgenda.model.Usuario;
import com.allgenda.repository.AvaliacaoRepository;
import com.allgenda.repository.DisciplinaRepository;
import com.allgenda.repository.ParticipacaoRepository;
import com.allgenda.repository.UsuarioRepository;
import com.allgenda.service.AuthService;
import com.allgenda.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
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

    private record Pessoa(Usuario usuario, String autorizacao) {}

    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy filtros;
    @Autowired UsuarioService usuarios;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired AuthService auth;
    @Autowired DisciplinaRepository disciplinas;
    @Autowired ParticipacaoRepository participacoes;
    @Autowired AvaliacaoRepository avaliacoes;

    private MockMvc mvc;
    private Pessoa dono;
    private UUID disciplinaId;

    @BeforeEach
    void preparar() {
        mvc = webAppContextSetup(context).addFilters(filtros).build();
        dono = novaPessoa("Dono");
        Disciplina disciplina = novaDisciplina("PDS");
        participar(dono, disciplina, PapelParticipacao.DONO, StatusParticipacao.ACEITO);
        disciplinaId = disciplina.getId();
    }

    private Pessoa novaPessoa(String prefixo) {
        String sufixo = UUID.randomUUID().toString();
        String email = prefixo.toLowerCase() + "-" + sufixo + "@example.com";
        UsuarioResponseDTO criado = usuarios.cadastrar(new UsuarioRequestDTO(prefixo + " " + sufixo, email, "senha-forte-123"));
        String autorizacao = "Bearer " + auth.login(new LoginRequestDTO(email, "senha-forte-123")).token();
        return new Pessoa(usuarioRepository.findById(criado.id()).orElseThrow(), autorizacao);
    }

    private Disciplina novaDisciplina(String nome) {
        Disciplina disciplina = new Disciplina();
        disciplina.setNome(nome);
        return disciplinas.saveAndFlush(disciplina);
    }

    private void participar(Pessoa pessoa, Disciplina disciplina, PapelParticipacao papel, StatusParticipacao status) {
        Participacao participacao = new Participacao();
        participacao.setUsuario(pessoa.usuario());
        participacao.setDisciplina(disciplina);
        participacao.setPapel(papel);
        participacao.setStatus(status);
        participacoes.saveAndFlush(participacao);
    }

    private String corpo(UUID disciplina, String descricao, String tipo) {
        return """
                {"disciplinaId":"%s","descricao":"%s","data":"2026-11-01","tipo":"%s"}
                """.formatted(disciplina, descricao, tipo);
    }

    private ResultActions cadastrar(Pessoa quem, String json) throws Exception {
        return mvc.perform(post("/api/avaliacoes").header("Authorization", quem.autorizacao())
                .contentType("application/json").content(json));
    }

    private ResultActions listar(Pessoa quem, UUID disciplina) throws Exception {
        return mvc.perform(get("/api/avaliacoes").header("Authorization", quem.autorizacao())
                .param("disciplinaId", disciplina.toString()));
    }

    @Test
    void cadastraProvaEEntregaEListaSomenteDisciplinaSolicitada() throws Exception {
        Disciplina outra = novaDisciplina("Outra disciplina");
        participar(dono, outra, PapelParticipacao.DONO, StatusParticipacao.ACEITO);
        for (TipoAvaliacao tipo : TipoAvaliacao.values()) {
            cadastrar(dono, corpo(disciplinaId, "Avaliação de PDS", tipo.name()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.tipo").value(tipo.name()))
                    .andExpect(jsonPath("$.disciplinaId").value(disciplinaId.toString()));
        }
        cadastrar(dono, corpo(outra.getId(), "Outra", "PROVA")).andExpect(status().isCreated());
        listar(dono, disciplinaId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].disciplinaNome").value("PDS"));
        assertThat(avaliacoes.findByDisciplinaIdOrderByDataAscIdAsc(disciplinaId)).hasSize(2);
    }

    @Test
    void membroAceitoCadastraEListaAvaliacoes() throws Exception {
        Pessoa membro = novaPessoa("Membro");
        participar(membro, disciplinas.findById(disciplinaId).orElseThrow(), PapelParticipacao.MEMBRO, StatusParticipacao.ACEITO);

        cadastrar(membro, corpo(disciplinaId, "Prova do membro", "PROVA")).andExpect(status().isCreated());
        listar(membro, disciplinaId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        // o dono enxerga a avaliação que o membro cadastrou
        listar(dono, disciplinaId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void quemNaoParticipaRecebe403NoCadastroENaListagem() throws Exception {
        Pessoa estranho = novaPessoa("Estranho");

        cadastrar(estranho, corpo(disciplinaId, "Prova", "PROVA")).andExpect(status().isForbidden());
        listar(estranho, disciplinaId).andExpect(status().isForbidden());
        assertThat(avaliacoes.count()).isZero();
    }

    @Test
    void convitePendenteOuRecusadoRecebe403() throws Exception {
        Disciplina disciplina = disciplinas.findById(disciplinaId).orElseThrow();
        for (StatusParticipacao status : new StatusParticipacao[] {StatusParticipacao.PENDENTE, StatusParticipacao.RECUSADO}) {
            Pessoa convidado = novaPessoa("Convidado");
            participar(convidado, disciplina, PapelParticipacao.MEMBRO, status);

            cadastrar(convidado, corpo(disciplinaId, "Prova", "PROVA")).andExpect(status().isForbidden());
            listar(convidado, disciplinaId).andExpect(status().isForbidden());
        }
        assertThat(avaliacoes.count()).isZero();
    }

    @Test
    void semTokenRecebe401() throws Exception {
        mvc.perform(get("/api/avaliacoes").param("disciplinaId", disciplinaId.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disciplinaInexistenteRetorna404NoCadastroENaListagem() throws Exception {
        UUID inexistente = UUID.randomUUID();
        cadastrar(dono, corpo(inexistente, "Prova", "PROVA")).andExpect(status().isNotFound());
        listar(dono, inexistente).andExpect(status().isNotFound());
    }

    @Test
    void rejeitaTipoInvalidoDescricaoVaziaECamposAusentes() throws Exception {
        for (String json : new String[] {
                corpo(disciplinaId, "Prova", "INVALIDO"),
                corpo(disciplinaId, "   ", "PROVA"),
                "{}",
                corpo(disciplinaId, "Prova", "PROVA").replace("\"2026-11-01\"", "null"),
                corpo(disciplinaId, "Prova", "PROVA").replace("\"PROVA\"", "null")}) {
            cadastrar(dono, json).andExpect(status().isBadRequest());
        }
        assertThat(avaliacoes.count()).isZero();
        listar(dono, disciplinaId)
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}