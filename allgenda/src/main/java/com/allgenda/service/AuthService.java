package com.allgenda.service;

import com.allgenda.dto.request.*;
import com.allgenda.dto.response.*;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.model.Usuario;
import com.allgenda.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final JwtEncoder tokens;
    public AuthService(UsuarioRepository usuarios, PasswordEncoder senhas, JwtEncoder tokens) {
        this.usuarios = usuarios; this.senhas = senhas; this.tokens = tokens;
    }
    private String email(String email) {
        if (email == null || email.length() > 254 || !email.trim().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new RequisicaoInvalidaException("Informe um e-mail válido.");
        return email.trim().toLowerCase(Locale.ROOT);
    }
    @Transactional
    public UsuarioResponseDTO cadastrar(UsuarioRequestDTO dto) {
        String email = email(dto.email());
        if (dto.nome() == null || dto.nome().isBlank() || dto.nome().trim().length() > 255)
            throw new RequisicaoInvalidaException("Informe um nome de até 255 caracteres.");
        if (dto.senha() == null || dto.senha().length() < 8 || dto.senha().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new RequisicaoInvalidaException("A senha deve ter ao menos 8 caracteres e no máximo 72 bytes UTF-8.");
        if (usuarios.existsByEmail(email) || usuarios.existsByNome(dto.nome().trim()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nome ou e-mail já cadastrado.");
        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome().trim()); usuario.setEmail(email);
        usuario.setSenhaHash(senhas.encode(dto.senha()));
        return resposta(usuarios.saveAndFlush(usuario));
    }
    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO dto) {
        String email = email(dto.email());
        Usuario usuario = usuarios.findByEmail(email).orElse(null);
        if (usuario == null || usuario.getSenhaHash() == null || dto.senha() == null
                || dto.senha().getBytes(StandardCharsets.UTF_8).length > 72
                || !senhas.matches(dto.senha(), usuario.getSenhaHash()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("allgenda").subject(usuario.getId().toString())
                .issuedAt(agora).expiresAt(agora.plusSeconds(3600)).build();
        String token = tokens.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginResponseDTO(token, "Bearer", 3600, resposta(usuario));
    }
    private UsuarioResponseDTO resposta(Usuario usuario) {
        return new UsuarioResponseDTO(usuario.getId(), usuario.getNome(), usuario.getEmail());
    }
}
