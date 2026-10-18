package com.allgenda.config;

import com.allgenda.dto.request.UsuarioRequestDTO;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.repository.UsuarioRepository;
import com.allgenda.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

// Somente desenvolvimento: garante que exista um usuário de teste para fazer login, inclusive num banco recém-criado.
// As credenciais vêm das variáveis DEV_USUARIO_* (definidas no mise.toml); sem elas, nada é criado.
@Component
public class UsuarioTesteInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UsuarioTesteInitializer.class);

    private final UsuarioRepository usuarios;
    private final AuthService auth;
    private final String nome;
    private final String email;
    private final String senha;

    public UsuarioTesteInitializer(UsuarioRepository usuarios,
                                   AuthService auth,
                                   @Value("${app.dev.usuario.nome:}") String nome,
                                   @Value("${app.dev.usuario.email:}") String email,
                                   @Value("${app.dev.usuario.senha:}") String senha) {
        this.usuarios = usuarios;
        this.auth = auth;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank()) {
            return;
        }
        // mesma normalização do AuthService, para reconhecer o usuário já criado numa inicialização anterior
        if (usuarios.existsByEmail(email.trim().toLowerCase(Locale.ROOT))) {
            return;
        }
        try {
            auth.cadastrar(new UsuarioRequestDTO(nome, email, senha)); // mesma validação e mesmo hash do cadastro normal
            log.info("Usuário de teste criado: {}", email);
        } catch (RequisicaoInvalidaException | ResponseStatusException e) {
            // dados inválidos ou nome já usado por outro e-mail não devem impedir o backend de subir
            log.warn("Não foi possível criar o usuário de teste {}: {}", email, e.getMessage());
        }
    }
}
