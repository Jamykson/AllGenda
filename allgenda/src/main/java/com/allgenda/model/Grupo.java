package com.allgenda.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

// Ponto fixo do framework: um grupo de pessoas que se reúnem em encontros, registram eventos e escrevem
// anotações. É abstrato: cada aplicação cria a sua subclasse (no AllGenda, Disciplina) e uma fábrica
// para ela. Herança JOINED: os campos comuns ficam em "grupos" e os da subclasse, na tabela dela.
@Entity
@Table(name = "grupos")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Grupo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nome")
    private String nome;

    @Column(name = "descricao", length = 500)
    private String descricao;

    @OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Encontro> encontros = new ArrayList<>();

    @OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Evento> eventos = new ArrayList<>();

    // as tags são do grupo: excluí-lo apaga as tags junto
    @OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tag> tags = new ArrayList<>();

    // Associação independente: persistência e remoção de participações são explícitas no serviço.
    @OneToMany(mappedBy = "grupo")
    private List<Participacao> participacoes = new ArrayList<>();

    // nome do tipo no registro de tipos (ex.: "DISCIPLINA"), definido pela subclasse
    public abstract String tipo();

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public List<Encontro> getEncontros() {
        return encontros;
    }

    public List<Evento> getEventos() {
        return eventos;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public List<Participacao> getParticipacoes() {
        return participacoes;
    }

    // Mantém os dois lados da relação coerentes: a lista do grupo e o campo grupo da participação
    public void addParticipacao(Participacao participacao) {
        participacoes.add(participacao);
        participacao.setGrupo(this);
    }
}
