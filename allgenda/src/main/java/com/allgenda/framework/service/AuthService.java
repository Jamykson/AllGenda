package com.allgenda.framework.service;

import com.allgenda.framework.dto.request.LoginRequestDTO;
import com.allgenda.framework.dto.response.LoginResponseDTO;

public interface AuthService {
    LoginResponseDTO login(LoginRequestDTO dto);
}
