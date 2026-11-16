package com.allgenda.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// Grupo do AllGenda: uma disciplina, com as suas aulas, avaliações e participantes.
@Entity
@Table(name = "disciplinas")
public class Disciplina extends Grupo {

    public static final String TIPO = "DISCIPLINA";

    @Override
    public String tipo() {
        return TIPO;
    }
}
