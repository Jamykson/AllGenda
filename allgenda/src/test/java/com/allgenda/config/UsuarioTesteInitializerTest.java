package com.allgenda.config;

import com.allgenda.dto.request.UsuarioRequestDTO;
import com.allgenda.repository.UsuarioRepository;
import com.allgenda.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioTesteInitializerTest {

    @Mock
    private UsuarioRepository usuarios;

    @Mock
    private AuthService auth;

    private UsuarioTesteInitializer inicializador(String email) {
        return new UsuarioTesteInitializer(usuarios, auth, "Aluno Teste", email, "senha-de-teste-123");
    }

    @Test
    void semEmailConfiguradoNaoFazNada() {
        inicializador("").run(null);

        verifyNoInteractions(usuarios, auth);
    }

    @Test
    void usuarioJaExistenteNaoEhCriadoDeNovo() {
        when(usuarios.existsByEmail("aluno@allgenda.test")).thenReturn(true);

        inicializador(" Aluno@Allgenda.test ").run(null);

        verify(auth, never()).cadastrar(any());
    }

    @Test
    void usuarioAusenteEhCadastradoPeloAuthService() {
        inicializador("aluno@allgenda.test").run(null);

        verify(auth).cadastrar(new UsuarioRequestDTO("Aluno Teste", "aluno@allgenda.test", "senha-de-teste-123"));
    }

    @Test
    void conflitoAoCadastrarNaoImpedeOBackendDeSubir() {
        when(auth.cadastrar(any())).thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Nome ou e-mail já cadastrado."));

        assertThatCode(() -> inicializador("aluno@allgenda.test").run(null)).doesNotThrowAnyException();
    }
}
