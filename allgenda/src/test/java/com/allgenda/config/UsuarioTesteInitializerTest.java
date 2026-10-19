package com.allgenda.config;

import com.allgenda.dto.request.UsuarioRequestDTO;
import com.allgenda.exception.RecursoJaExisteException;
import com.allgenda.repository.UsuarioRepository;
import com.allgenda.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private UsuarioService cadastroUsuarios;

    private UsuarioTesteInitializer inicializador(String email) {
        return new UsuarioTesteInitializer(usuarios, cadastroUsuarios, "Aluno Teste", email, "12345678");
    }

    @Test
    void semEmailConfiguradoNaoFazNada() {
        inicializador("").run(null);

        verifyNoInteractions(usuarios, cadastroUsuarios);
    }

    @Test
    void usuarioJaExistenteNaoEhCriadoDeNovo() {
        when(usuarios.existsByEmail("aluno@gmail.com")).thenReturn(true);

        inicializador(" Aluno@Gmail.com ").run(null);

        verify(cadastroUsuarios, never()).cadastrar(any());
    }

    @Test
    void usuarioAusenteEhCadastradoPeloUsuarioService() {
        inicializador("aluno@gmail.com").run(null);

        verify(cadastroUsuarios).cadastrar(new UsuarioRequestDTO("Aluno Teste", "aluno@gmail.com", "12345678"));
    }

    @Test
    void conflitoAoCadastrarNaoImpedeOBackendDeSubir() {
        when(cadastroUsuarios.cadastrar(any())).thenThrow(new RecursoJaExisteException("Nome ou e-mail já cadastrado."));

        assertThatCode(() -> inicializador("aluno@gmail.com").run(null)).doesNotThrowAnyException();
    }
}
