package com.allgenda.aplicacao.allgenda.dto.request;

import com.allgenda.framework.model.VisibilidadeAnotacao;

// corpo de PATCH /api/anotacoes/{id}/visibilidade; só o autor altera
public record AnotacaoVisibilidadeDTO(VisibilidadeAnotacao visibilidade) {}
