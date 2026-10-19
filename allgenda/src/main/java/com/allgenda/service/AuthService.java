package com.allgenda.service;

import com.allgenda.dto.request.LoginRequestDTO;
import com.allgenda.dto.response.LoginResponseDTO;

public interface AuthService {
    LoginResponseDTO login(LoginRequestDTO dto);
}
