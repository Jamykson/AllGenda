package com.allgenda.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "participacoes", uniqueConstraints = @UniqueConstraint(
        name = "uk_participacao_usuario_disciplina", columnNames = {"usuario_id", "disciplina_id"}))
public class Participacao {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "disciplina_id", nullable = false)
    private Disciplina disciplina;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PapelParticipacao papel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusParticipacao status;

    public UUID getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Disciplina getDisciplina() { return disciplina; }
    public void setDisciplina(Disciplina disciplina) { this.disciplina = disciplina; }
    public PapelParticipacao getPapel() { return papel; }
    public void setPapel(PapelParticipacao papel) { this.papel = papel; }
    public StatusParticipacao getStatus() { return status; }
    public void setStatus(StatusParticipacao status) { this.status = status; }
}
