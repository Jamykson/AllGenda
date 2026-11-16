package com.allgenda.aplicacao.allgenda.model;

import com.allgenda.framework.model.Encontro;

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
