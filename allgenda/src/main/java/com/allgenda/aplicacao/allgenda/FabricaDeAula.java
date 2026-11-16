package com.allgenda.aplicacao.allgenda;

import com.allgenda.framework.extensao.DadosDoEncontro;
import com.allgenda.framework.extensao.FabricaDeEncontro;
import com.allgenda.aplicacao.allgenda.model.Aula;
import com.allgenda.framework.model.Encontro;
import com.allgenda.framework.model.Grupo;

// O encontro do AllGenda é a aula; ela não tem campos além dos comuns, que o núcleo preenche.
public class FabricaDeAula implements FabricaDeEncontro {

    @Override
    public String tipo() {
        return Aula.TIPO;
    }

    @Override
    public Encontro criar(DadosDoEncontro dados, Grupo grupo) {
        return new Aula();
    }
}
