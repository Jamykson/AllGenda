package com.allgenda.framework.service.impl;

import com.allgenda.framework.dto.request.LoginRequestDTO;
import com.allgenda.framework.dto.response.LoginResponseDTO;
import com.allgenda.framework.dto.response.UsuarioResponseDTO;
import com.allgenda.framework.exception.CredenciaisInvalidasException;
import com.allgenda.framework.exception.RequisicaoInvalidaException;
import com.allgenda.framework.model.Usuario;
import com.allgenda.framework.repository.UsuarioRepository;
import com.allgenda.framework.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final JwtEncoder tokens;

    public AuthServiceImpl(UsuarioRepository usuarios, PasswordEncoder senhas, JwtEncoder tokens) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.tokens = tokens;
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO dto) {
        String email = normalizarEmail(dto.email());
        Usuario usuario = usuarios.findByEmail(email).orElse(null);

        // A mesma mensagem evita revelar se o e-mail está cadastrado.
        if (usuario == null || usuario.getSenhaHash() == null || dto.senha() == null
                || dto.senha().getBytes(StandardCharsets.UTF_8).length > 72
                || !senhas.matches(dto.senha(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException();      // 401
        }

        // O subject identifica o usuário autenticado nos endpoints protegidos.
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("allgenda")
                .subject(usuario.getId().toString())
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(3600))
                .build();
        String token = tokens.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        UsuarioResponseDTO resposta = new UsuarioResponseDTO(usuario.getId(), usuario.getNome(), usuario.getEmail());
        return new LoginResponseDTO(token, "Bearer", 3600, resposta);
    }

    private String normalizarEmail(String email) {
        if (email == null || email.length() > 254 || !email.trim().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new RequisicaoInvalidaException("Informe um e-mail válido.");    // 400
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
