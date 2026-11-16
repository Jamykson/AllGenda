package com.allgenda.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

// Quando o grupo se encontra: dias da semana, horário e período. Cada grupo tem no máximo uma.
// Os encontros de uma recorrência não existem no banco até receberem a primeira anotação, imagem ou
// cancelamento (o front as gera a partir desta configuração e as cadastra nesse momento).
@Entity
@Table(name = "recorrencias")
public class Recorrencia implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id", nullable = false, unique = true)
    private Grupo grupo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recorrencia_dias", joinColumns = @JoinColumn(name = "recorrencia_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 3)
    private Set<DiaSemana> diasSemana = EnumSet.noneOf(DiaSemana.class);

    @Column(name = "hora_inicio", nullable = false, length = 5)
    private String horaInicio;

    @Column(name = "hora_fim", nullable = false, length = 5)
    private String horaFim;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FrequenciaRecorrencia frequencia = FrequenciaRecorrencia.SEMANAL;

    public UUID getId() {
        return id;
    }

    public Grupo getGrupo() {
        return grupo;
    }

    public void setGrupo(Grupo grupo) {
        this.grupo = grupo;
    }

    public Set<DiaSemana> getDiasSemana() {
        return diasSemana;
    }

    public void setDiasSemana(Set<DiaSemana> diasSemana) {
        this.diasSemana.clear();
        this.diasSemana.addAll(diasSemana);
    }

    public String getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(String horaInicio) {
        this.horaInicio = horaInicio;
    }

    public String getHoraFim() {
        return horaFim;
    }

    public void setHoraFim(String horaFim) {
        this.horaFim = horaFim;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public FrequenciaRecorrencia getFrequencia() {
        return frequencia;
    }

    public void setFrequencia(FrequenciaRecorrencia frequencia) {
        this.frequencia = frequencia;
    }
}
