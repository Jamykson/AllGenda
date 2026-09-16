package com.allgenda.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.UUID;


@Entity
@Table(name = "anotacoes")
public class Anotacao implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conteudo", columnDefinition = "TEXT")
    private String conteudo;

    @Column(name = "dataCriacao")
    private LocalDateTime dataCriacao;

    @ManyToOne(optional = false)
    @JoinColumn(name = "aula_id")
    private Aula aula;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario autor;

    /*/@ManyToMany
    @JoinTable(name = "anotacao_tag", 
        joinColumns = @JoinColumn(name = "anotacao_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();/*/ 
}