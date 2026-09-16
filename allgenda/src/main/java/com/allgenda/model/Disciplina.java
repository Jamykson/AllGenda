package com.allgenda.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.lang.annotation.Inherited;

@Entity
@Table(name = "Disciplinas")
public class Disciplina implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(name = "nome")
    private String nome;

    @OneToMany(mappedBy = "disciplina", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Aula> aulas = new ArrayList<>();
}