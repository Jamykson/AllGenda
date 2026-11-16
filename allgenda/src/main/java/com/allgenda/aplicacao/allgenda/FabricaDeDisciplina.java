package com.allgenda.aplicacao.allgenda;

import com.allgenda.framework.extensao.DadosDoGrupo;
import com.allgenda.framework.extensao.FabricaDeGrupo;
import com.allgenda.model.Disciplina;
import com.allgenda.model.Grupo;

// O grupo do AllGenda é a disciplina; ela não tem campos além dos comuns, que o núcleo preenche.
public class FabricaDeDisciplina implements FabricaDeGrupo {

    @Override
    public String tipo() {
        return Disciplina.TIPO;
    }

    @Override
    public Grupo criar(DadosDoGrupo dados) {
        return new Disciplina();
    }
}
