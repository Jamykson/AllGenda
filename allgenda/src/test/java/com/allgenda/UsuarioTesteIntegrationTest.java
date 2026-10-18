package com.allgenda;

import com.allgenda.config.UsuarioTesteInitializer;
import com.allgenda.dto.request.LoginRequestDTO;
import com.allgenda.repository.UsuarioRepository;
import com.allgenda.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

// banco H2 próprio, para o usuário criado na inicialização não aparecer nos testes que usam o banco compartilhado
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:usuario-teste;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "app.dev.usuario.nome=Aluno Teste",
        "app.dev.usuario.email=aluno@allgenda.test",
        "app.dev.usuario.senha=senha-de-teste-123"
})
class UsuarioTesteIntegrationTest {

    @Autowired UsuarioTesteInitializer inicializador;
    @Autowired UsuarioRepository usuarios;
    @Autowired AuthService auth;

    @Test
    void usuarioDeTesteEhCriadoNaInicializacaoEConsegueEntrar() {
        var usuario = usuarios.findByEmail("aluno@allgenda.test").orElseThrow();
        assertThat(usuario.getNome()).isEqualTo("Aluno Teste");

        var login = auth.login(new LoginRequestDTO("aluno@allgenda.test", "senha-de-teste-123"));
        assertThat(login.usuario().id()).isEqualTo(usuario.getId());
    }

    @Test
    void reiniciarNaoDuplicaOUsuarioDeTeste() {
        long antes = usuarios.count();

        inicializador.run(null);

        assertThat(usuarios.count()).isEqualTo(antes);
        assertThat(usuarios.existsByEmail("aluno@allgenda.test")).isTrue();
    }
}
