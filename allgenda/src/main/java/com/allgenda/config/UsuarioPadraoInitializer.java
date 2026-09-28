package com.allgenda.config;

import com.allgenda.model.Usuario;
import com.allgenda.repository.UsuarioRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/*/ Solução temporária até a sprint 3 (usuários/login): garante que exista um usuário padrão no banco,
    usado como autor das anotações quando a requisição não informa autorId. /*/
@Component
public class UsuarioPadraoInitializer implements ApplicationRunner {

    public static final String NOME_USUARIO_PADRAO = "Usuário padrão";

    private final UsuarioRepository usuarioRepository;

    public UsuarioPadraoInitializer(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.findByNome(NOME_USUARIO_PADRAO).isEmpty()) {
            Usuario usuario = new Usuario();
            usuario.setNome(NOME_USUARIO_PADRAO);
            usuarioRepository.save(usuario);
        }
    }
}
