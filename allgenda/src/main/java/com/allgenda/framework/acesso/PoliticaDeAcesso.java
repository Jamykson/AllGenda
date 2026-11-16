package com.allgenda.framework.acesso;

import java.util.Set;

import com.allgenda.framework.model.StatusParticipacao;

// Ponto variável 2 (Strategy): quem pode fazer o quê num grupo. A aplicação declara os papéis, o papel de
// quem cria o grupo e de quem é convidado, o status de quem ingressa e quais papéis podem cada Acao.
// Regras que não dependem da aplicação (só o autor mexe na própria anotação, anotação privada só o autor
// vê, quem criou o grupo não sai dele) ficam fixas no núcleo e não passam por aqui.
public interface PoliticaDeAcesso {

    Set<Papel> papeis();

    // papel de quem cria o grupo; a participação dele já nasce ACEITO
    Papel papelDoCriador();

    // papel de quem é convidado para o grupo
    Papel papelDoConvidado();

    boolean permite(Papel papel, Acao acao);

    // status da participação de quem é convidado (no AllGenda, PENDENTE: o convidado precisa aceitar)
    StatusParticipacao statusAoIngressar();
}
