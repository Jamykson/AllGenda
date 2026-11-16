package com.allgenda.framework.service;

import com.allgenda.framework.dto.request.UsuarioRequestDTO;
import com.allgenda.framework.dto.response.UsuarioResponseDTO;

public interface UsuarioService {
    UsuarioResponseDTO cadastrar(UsuarioRequestDTO dto);
}
