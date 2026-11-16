package com.allgenda.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

// Evento do AllGenda: uma avaliação da disciplina, que é uma prova ou uma entrega (TipoAvaliacao).
@Entity
@Table(name = "avaliacoes")
public class Avaliacao extends Evento {

    public static final String TIPO = "AVALIACAO";

    // o tipo de avaliação (PROVA ou ENTREGA); não confundir com tipo(), que é o nome do tipo no registro
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAvaliacao tipo;

    @Override
    public String tipo() {
        return TIPO;
    }

    @Override
    public String subtipo() {
        return tipo.name();
    }

    public TipoAvaliacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoAvaliacao tipo) {
        this.tipo = tipo;
    }
}
