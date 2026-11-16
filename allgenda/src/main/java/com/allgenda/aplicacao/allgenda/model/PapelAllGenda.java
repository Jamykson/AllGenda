package com.allgenda.aplicacao.allgenda.model;

import com.allgenda.framework.acesso.Papel;

// Os papéis do AllGenda: quem cria a disciplina é o DONO; quem entra por convite é MEMBRO.
public final class PapelAllGenda {

    public static final Papel DONO = new Papel("DONO");
    public static final Papel MEMBRO = new Papel("MEMBRO");

    private PapelAllGenda() {
    }
}
