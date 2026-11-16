package com.allgenda.aplicacao.allgenda;

import com.allgenda.framework.exception.RequisicaoInvalidaException;
import com.allgenda.framework.extensao.DadosDoEvento;
import com.allgenda.framework.extensao.FabricaDeEvento;
import com.allgenda.aplicacao.allgenda.model.Avaliacao;
import com.allgenda.framework.model.Evento;
import com.allgenda.framework.model.Grupo;
import com.allgenda.aplicacao.allgenda.model.TipoAvaliacao;

// O evento do AllGenda é a avaliação. A validação do que é dela fica aqui: o subtipo precisa ser um
// TipoAvaliacao (PROVA ou ENTREGA).
public class FabricaDeAvaliacao implements FabricaDeEvento {

    @Override
    public String tipo() {
        return Avaliacao.TIPO;
    }

    @Override
    public Evento criar(DadosDoEvento dados, Grupo grupo) {
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setTipo(tipoDaAvaliacao(dados.subtipo()));
        return avaliacao;
    }

    private static TipoAvaliacao tipoDaAvaliacao(String subtipo) {
        try {
            return TipoAvaliacao.valueOf(subtipo);
        } catch (NullPointerException | IllegalArgumentException e) {
            throw new RequisicaoInvalidaException("O tipo da avaliação é obrigatório: PROVA ou ENTREGA.");
        }
    }
}
