package com.allgenda.service;

import com.allgenda.dto.request.UsuarioRequestDTO;
import com.allgenda.dto.response.UsuarioResponseDTO;

public interface UsuarioService {
    UsuarioResponseDTO cadastrar(UsuarioRequestDTO dto);
}
