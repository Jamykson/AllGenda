package com.allgenda.service.impl;

import com.allgenda.dto.request.UsuarioRequestDTO;
import com.allgenda.dto.response.UsuarioResponseDTO;
import com.allgenda.exception.RecursoJaExisteException;
import com.allgenda.exception.RequisicaoInvalidaException;
import com.allgenda.model.Usuario;
import com.allgenda.repository.UsuarioRepository;
import com.allgenda.service.UsuarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;

    public UsuarioServiceImpl(UsuarioRepository usuarios, PasswordEncoder senhas) {
        this.usuarios = usuarios;
        this.senhas = senhas;
    }

    @Override
    @Transactional
    public UsuarioResponseDTO cadastrar(UsuarioRequestDTO dto) {
        String email = normalizarEmail(dto.email());
        validarCadastro(dto);

        // Antecipamos conflitos; a restrição do banco também protege cadastros simultâneos.
        if (usuarios.existsByEmail(email) || usuarios.existsByNome(dto.nome().trim())) {
            throw new RecursoJaExisteException("Nome ou e-mail já cadastrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome().trim());
        usuario.setEmail(email);
        // Apenas o hash BCrypt é persistido e a resposta não expõe credenciais.
        usuario.setSenhaHash(senhas.encode(dto.senha()));
        Usuario salvo = usuarios.saveAndFlush(usuario);
        return new UsuarioResponseDTO(salvo.getId(), salvo.getNome(), salvo.getEmail());
    }

    private void validarCadastro(UsuarioRequestDTO dto) {
        if (dto.nome() == null || dto.nome().isBlank() || dto.nome().trim().length() > 255) {
            throw new RequisicaoInvalidaException("Informe um nome de até 255 caracteres.");
        }
        if (dto.senha() == null || dto.senha().length() < 8
                || dto.senha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RequisicaoInvalidaException("A senha deve ter ao menos 8 caracteres e no máximo 72 bytes UTF-8.");
        }
    }

    private String normalizarEmail(String email) {
        if (email == null || email.length() > 254 || !email.trim().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new RequisicaoInvalidaException("Informe um e-mail válido.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
