package com.allgenda.dto.request;

public record AnotacaoUpdateDTO(String conteudo) {} 

// DTO específico para edição de anotações, ao invés de reaproveitar o AnotacaoRequestDTO que tem muitos campos desnecessários para edição.