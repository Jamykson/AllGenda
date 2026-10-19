package com.allgenda;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import com.allgenda.dto.request.LoginRequestDTO;
import com.allgenda.dto.request.UsuarioRequestDTO;
import com.allgenda.model.*;
import com.allgenda.repository.*;
import com.allgenda.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.transaction.annotation.Transactional;
import com.allgenda.exception.CredenciaisInvalidasException;
import com.allgenda.service.UsuarioService;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthIntegrationTest {
    @Autowired org.springframework.web.context.WebApplicationContext context;
    @Autowired org.springframework.security.web.FilterChainProxy filtros;
    @Autowired AuthService auth;
    @Autowired UsuarioService cadastroUsuarios;
    @Autowired UsuarioRepository usuarios;
    @Autowired DisciplinaRepository disciplinas;
    @Autowired ParticipacaoRepository participacoes;
    @Autowired JwtDecoder decoder;
    @Autowired PasswordEncoder senhas;

    @Test
    void endpointProtegidoSemTokenRecebe401() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context)
                .addFilters(filtros).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/anotacoes")
                .param("tag", "java"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void cadastroPublicoELoginMantemRespostasEErrosPadronizados() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context)
                .addFilters(filtros).build();
        String cadastro = """
                {"nome":"Carlos","email":"carlos@example.com","senha":"senha-forte-123"}
                """;
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/usuarios")
                .contentType("application/json").content(cadastro))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.email").value("carlos@example.com"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.senha").doesNotExist())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.senhaHash").doesNotExist());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/usuarios")
                .contentType("application/json").content(cadastro))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value(409));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                .contentType("application/json").content("""
                        {"email":"carlos@example.com","senha":"senha-errada"}
                        """))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.detail").value("E-mail ou senha inválidos."));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                .contentType("application/json").content("""
                        {"email":"carlos@example.com","senha":"senha-forte-123"}
                        """))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.token").isNotEmpty());
    }

    @Test
    void cadastroArmazenaHashELoginEmiteTokenComIdentidade() {
        var usuario = cadastroUsuarios.cadastrar(new UsuarioRequestDTO("Maria", " MARIA@example.com ", "senha-forte-123"));
        var persistido = usuarios.findById(usuario.id()).orElseThrow();
        assertThat(persistido.getEmail()).isEqualTo("maria@example.com");
        assertThat(persistido.getSenhaHash()).isNotEqualTo("senha-forte-123");
        assertThat(senhas.matches("senha-forte-123", persistido.getSenhaHash())).isTrue();
        var login = auth.login(new LoginRequestDTO("maria@example.com", "senha-forte-123"));
        var jwt = decoder.decode(login.token());
        assertThat(jwt.getSubject()).isEqualTo(usuario.id().toString());
        assertThat(jwt.getExpiresAt()).isEqualTo(jwt.getIssuedAt().plusSeconds(3600));
        assertThatThrownBy(() -> auth.login(new LoginRequestDTO("maria@example.com", "senha-errada")))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void assinaturaComOutroSegredoNaoEhAceita() {
        var usuario = cadastroUsuarios.cadastrar(new UsuarioRequestDTO("João", "joao@example.com", "senha-forte-123"));
        var login = auth.login(new LoginRequestDTO(usuario.email(), "senha-forte-123"));
        var key = new SecretKeySpec("outro-segredo-com-pelo-menos-32-bytes".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var outroDecoder = NimbusJwtDecoder.withSecretKey(key).build();
        assertThatThrownBy(() -> outroDecoder.decode(login.token())).isInstanceOf(JwtException.class);
    }

    @Test
    void participacaoPersisteComUsuarioEDisciplinaSemRemoverAssociados() {
        var cadastro = cadastroUsuarios.cadastrar(new UsuarioRequestDTO("Ana", "ana@example.com", "senha-forte-123"));
        var usuario = usuarios.findById(cadastro.id()).orElseThrow();
        var disciplina = new Disciplina();
        disciplina.setNome("PDS");
        disciplina = disciplinas.saveAndFlush(disciplina);
        var participacao = new Participacao();
        participacao.setUsuario(usuario);
        participacao.setDisciplina(disciplina);
        participacao.setPapel(PapelParticipacao.MEMBRO);
        participacao.setStatus(StatusParticipacao.PENDENTE);
        participacoes.saveAndFlush(participacao);
        assertThat(participacoes.findByUsuarioIdAndDisciplinaId(usuario.getId(), disciplina.getId())).isPresent();
        participacoes.delete(participacao);
        participacoes.flush();
        assertThat(usuarios.existsById(usuario.getId())).isTrue();
        assertThat(disciplinas.existsById(disciplina.getId())).isTrue();
    }
}
