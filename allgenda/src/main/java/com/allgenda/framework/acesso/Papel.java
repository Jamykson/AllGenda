package com.allgenda.framework.acesso;

// Papel de uma pessoa num grupo. Não é um enum fixo: cada aplicação declara os seus (no AllGenda, DONO e
// MEMBRO) na PoliticaDeAcesso. Dois papéis são iguais quando têm o mesmo código, que é o texto gravado no banco.
public record Papel(String codigo) {

    public Papel {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("O código do papel é obrigatório.");
        }
    }

    @Override
    public String toString() {
        return codigo;
    }
}
