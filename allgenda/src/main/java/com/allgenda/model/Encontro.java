package com.allgenda.model;

import java.io.Serializable;
import java.time.LocalDate;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

// Ponto fixo do framework: um encontro do grupo, numa data e horário, com as anotações e as imagens
// avulsas dele. Abstrato: a aplicação cria a subclasse (no AllGenda, Aula).
@Entity
@Table(name = "encontros")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Encontro implements Serializable {

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

    // encontro cancelado continua no grupo, com as anotações; só aparece marcado como cancelado
    @Column(name = "cancelada", nullable = false, columnDefinition = "boolean default false")
    private boolean cancelada = false;

    @ManyToOne(optional = false)
    @JoinColumn(name = "grupo_id")
    private Grupo grupo;

    @OneToMany(mappedBy = "encontro", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Anotacao> anotacoes = new ArrayList<>();

    // imagens avulsas, sem anotação; tirar uma desta lista apaga o registro (o arquivo é apagado pelo service)
    @OneToMany(mappedBy = "encontro", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImagemEncontro> imagens = new ArrayList<>();

    // nome do tipo no registro de tipos (ex.: "AULA"), definido pela subclasse
    public abstract String tipo();

    public UUID getId() {
        return id;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    public String getTopico() {
        return topico;
    }

    public void setTopico(String topico) {
        this.topico = topico;
    }

    public boolean isCancelada() {
        return cancelada;
    }

    public void setCancelada(boolean cancelada) {
        this.cancelada = cancelada;
    }

    public Grupo getGrupo() {
        return grupo;
    }

    public void setGrupo(Grupo grupo) {
        this.grupo = grupo;
    }

    public List<Anotacao> getAnotacoes() {
        return anotacoes;
    }

    public List<ImagemEncontro> getImagens() {
        return imagens;
    }

    public void addImagem(ImagemEncontro imagem) {
        imagem.setEncontro(this);
        this.imagens.add(imagem);
    }

    public void removeImagem(ImagemEncontro imagem) {
        this.imagens.remove(imagem);
    }
}
