package com.allgenda.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// Imagem avulsa de uma aula, sem anotação, com legenda opcional.
// Como em ImagemAnotacao, os bytes ficam no ArmazenamentoService, sob a chaveArmazenamento.
@Entity
@Table(name = "imagens_aula")
public class ImagemAula implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "aula_id")
    private Aula aula;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario autor;

    @Column(name = "legenda", length = 300)
    private String legenda;

    @Column(name = "nome_original", nullable = false)
    private String nomeOriginal;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "tamanho", nullable = false)
    private long tamanho;

    @Column(name = "chave_armazenamento", nullable = false, unique = true)
    private String chaveArmazenamento;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    // ordem na galeria da aula, a mesma para todos; o default permite criar a coluna numa tabela com linhas
    @Column(name = "posicao", nullable = false, columnDefinition = "integer default 0")
    private int posicao;

    public UUID getId() {
        return id;
    }

    public Aula getAula() {
        return aula;
    }

    public void setAula(Aula aula) {
        this.aula = aula;
    }

    public Usuario getAutor() {
        return autor;
    }

    public void setAutor(Usuario autor) {
        this.autor = autor;
    }

    public String getLegenda() {
        return legenda;
    }

    public void setLegenda(String legenda) {
        this.legenda = legenda;
    }

    public String getNomeOriginal() {
        return nomeOriginal;
    }

    public void setNomeOriginal(String nomeOriginal) {
        this.nomeOriginal = nomeOriginal;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getTamanho() {
        return tamanho;
    }

    public void setTamanho(long tamanho) {
        this.tamanho = tamanho;
    }

    public String getChaveArmazenamento() {
        return chaveArmazenamento;
    }

    public void setChaveArmazenamento(String chaveArmazenamento) {
        this.chaveArmazenamento = chaveArmazenamento;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public int getPosicao() {
        return posicao;
    }

    public void setPosicao(int posicao) {
        this.posicao = posicao;
    }
}
