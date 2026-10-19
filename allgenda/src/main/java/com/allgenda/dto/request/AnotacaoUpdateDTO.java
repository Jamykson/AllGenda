package com.allgenda.dto.request;

// substitui título e conteúdo: um campo ausente ou vazio fica sem valor, mas ao menos um dos dois é obrigatório
public record AnotacaoUpdateDTO(String titulo, String conteudo) {} 

// DTO específico para edição de anotações, ao invés de reaproveitar o AnotacaoRequestDTO que tem muitos campos desnecessários para edição.