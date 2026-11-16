package com.allgenda.framework.model;

import jakarta.persistence.*;
import java.util.UUID;

import com.allgenda.framework.acesso.Papel;
import com.allgenda.framework.acesso.PapelConverter;

@Entity
@Table(name = "participacoes", uniqueConstraints = @UniqueConstraint(
        name = "uk_participacao_usuario_grupo", columnNames = {"usuario_id", "grupo_id"}))
public class Participacao {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;

    // o papel é definido pela aplicação; no banco fica o código (ex.: "DONO"), como quando era um enum
    @Convert(converter = PapelConverter.class)
    @Column(nullable = false)
    private Papel papel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusParticipacao status;

    public UUID getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Grupo getGrupo() {
        return grupo;
    }

    public void setGrupo(Grupo grupo) {
        this.grupo = grupo;
    }

    public Papel getPapel() {
        return papel;
    }

    public void setPapel(Papel papel) {
        this.papel = papel;
    }

    public StatusParticipacao getStatus() {
        return status;
    }

    public void setStatus(StatusParticipacao status) {
        this.status = status;
    }
}
