
package com.allgenda.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "aulas")
public class Aula implements Serializable{

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "data")
    private LocalDate data;

    @Column(name = "horario")
    private String horario;

    @Column(name = "topico")
    private String topico;

    @ManyToOne(optional = false)
    @JoinColumn(name = "disciplina_id")
    private Disciplina disciplina;

    @OneToMany(mappedBy = "aula", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Anotacao> anotacoes = new ArrayList<>();
}