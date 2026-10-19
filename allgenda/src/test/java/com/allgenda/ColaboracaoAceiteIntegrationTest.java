package com.allgenda;

import com.allgenda.dto.request.UsuarioRequestDTO;
import com.allgenda.model.*;
import com.allgenda.repository.*;
import com.allgenda.service.UsuarioService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@Transactional
class ColaboracaoAceiteIntegrationTest {

    @Autowired UsuarioService cadastro;
    @Autowired UsuarioRepository usuarios;
    @Autowired DisciplinaRepository disciplinas;
    @Autowired ParticipacaoRepository participacoes;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtEncoder encoder;
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy filtros;

    private Usuario usuario() {
        String sufixo = UUID.randomUUID().toString();
        var resposta = cadastro.cadastrar(new UsuarioRequestDTO("Aluno " + sufixo,
                sufixo + "@example.com", "senha-forte-123"));
        return usuarios.findById(resposta.id()).orElseThrow();
    }

    private Disciplina disciplina() {
        Disciplina disciplina = new Disciplina();
        disciplina.setNome("PDS");
        return disciplinas.saveAndFlush(disciplina);
    }

    @Test
    void insertSqlManualFuncionaEAsDuasChavesEstrangeirasSaoObrigatorias() {
        Usuario usuario = usuario();
        Disciplina disciplina = disciplina();
        UUID id = UUID.randomUUID();
        String sql = "INSERT INTO participacoes (id, usuario_id, disciplina_id, papel, status) VALUES (?, ?, ?, ?, ?)";
        assertThat(jdbc.update(sql, id, usuario.getId(), disciplina.getId(), "MEMBRO", "PENDENTE")).isEqualTo(1);
        assertThat(participacoes.findById(id)).isPresent();
        assertThatThrownBy(() -> jdbc.update(sql, UUID.randomUUID(), UUID.randomUUID(), disciplina.getId(), "MEMBRO", "PENDENTE"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(sql, UUID.randomUUID(), usuario.getId(), UUID.randomUUID(), "MEMBRO", "PENDENTE"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void retirarParticipacaoDaColecaoNaoApagaAAssociacao() {
        Usuario usuario = usuario();
        Disciplina disciplina = disciplina();
        Participacao participacao = new Participacao();
        participacao.setUsuario(usuario);
        participacao.setDisciplina(disciplina);
        participacao.setPapel(PapelParticipacao.MEMBRO);
        participacao.setStatus(StatusParticipacao.PENDENTE);
        participacao = participacoes.saveAndFlush(participacao);
        UUID participacaoId = participacao.getId();
        UUID disciplinaId = disciplina.getId();
        entityManager.clear();

        Disciplina carregada = disciplinas.findById(disciplinaId).orElseThrow();
        assertThat(carregada.getParticipacoes()).hasSize(1);
        carregada.getParticipacoes().clear();
        entityManager.flush();
        entityManager.clear();

        // O lado inverso não controla o ciclo de vida da associação.
        assertThat(participacoes.findById(participacaoId)).isPresent();
        assertThat(usuarios.existsById(usuario.getId())).isTrue();
    }

    @Test
    void tokenExpiradoRecebe401() throws Exception {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("allgenda")
                .subject(usuario().getId().toString()).issuedAt(agora.minusSeconds(7200))
                .expiresAt(agora.minusSeconds(120)).build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        webAppContextSetup(context).addFilters(filtros).build()
                .perform(get("/api/anotacoes").param("tag", "java")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
