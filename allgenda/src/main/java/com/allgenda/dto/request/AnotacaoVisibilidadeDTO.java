package com.allgenda.dto.request;

import com.allgenda.model.VisibilidadeAnotacao;

// corpo de PATCH /api/anotacoes/{id}/visibilidade; só o autor altera
public record AnotacaoVisibilidadeDTO(VisibilidadeAnotacao visibilidade) {}
