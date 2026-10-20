package com.allgenda.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

// Tag de anotação. Cada disciplina tem as suas: "Prova" em duas disciplinas são duas tags,
// e renomear ou excluir uma não afeta a outra. Dentro da disciplina, o nome não se repete
// (sem diferenciar maiúsculas, regra garantida pelo TagService).
@Entity
@Table(name = "tags", uniqueConstraints = @UniqueConstraint(columnNames = {"disciplina_id", "nome"}))
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    // anulável só no banco: as tags antigas, globais, ainda não têm disciplina quando a coluna é criada.
    // A MigracaoTagsPorDisciplina as distribui ao iniciar, e o TagService sempre preenche a disciplina.
    @ManyToOne
    @JoinColumn(name = "disciplina_id")
    private Disciplina disciplina;

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Disciplina getDisciplina() {
        return disciplina;
    }

    public void setDisciplina(Disciplina disciplina) {
        this.disciplina = disciplina;
    }
}
