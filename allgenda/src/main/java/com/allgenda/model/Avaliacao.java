package com.allgenda.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "avaliacoes")
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String descricao;

    @Column(nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAvaliacao tipo;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "disciplina_id", nullable = false)
    private Disciplina disciplina;

    public UUID getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public TipoAvaliacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoAvaliacao tipo) {
        this.tipo = tipo;
    }

    public Disciplina getDisciplina() {
        return disciplina;
    }

    public void setDisciplina(Disciplina disciplina) {
        this.disciplina = disciplina;
    }
}
