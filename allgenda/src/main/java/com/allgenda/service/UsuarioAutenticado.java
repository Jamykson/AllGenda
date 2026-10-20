package com.allgenda.service;

import java.util.UUID;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Component;

import com.allgenda.exception.UsuarioNaoEncontradoException;
import com.allgenda.model.Usuario;
import com.allgenda.repository.UsuarioRepository;

// Responde "quem é o usuário logado?" a partir do token JWT da requisição atual.
@Component
public class UsuarioAutenticado {
    private final UsuarioRepository usuarioRepository;

    public UsuarioAutenticado(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // o "name" da autenticação é o subject do token, que é o id do usuário (definido no login)
    public UUID id() {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Autenticação de usuário obrigatória");  // 401
        }
        return UUID.fromString(autenticacao.getName());
    }

    public Usuario obter() {
        UUID id = id();
        return usuarioRepository.findById(id)
                        .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }
}
