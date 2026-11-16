package com.allgenda.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// Encontro do AllGenda: uma aula da disciplina.
@Entity
@Table(name = "aulas")
public class Aula extends Encontro {

    public static final String TIPO = "AULA";

    @Override
    public String tipo() {
        return TIPO;
    }
}
