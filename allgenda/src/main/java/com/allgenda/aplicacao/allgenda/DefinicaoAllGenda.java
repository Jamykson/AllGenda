package com.allgenda.aplicacao.allgenda;

import org.springframework.stereotype.Component;

import com.allgenda.framework.acesso.PoliticaDeAcesso;
import com.allgenda.framework.assistente.ComportamentoDoAssistente;
import com.allgenda.framework.extensao.DefinicaoDaAplicacao;
import com.allgenda.framework.extensao.RegistroDeTipos;

// A aplicação 1, AllGenda, instanciada sobre o framework: agenda acadêmica em que o grupo é a disciplina,
// o encontro é a aula e o evento é a avaliação (prova ou entrega). Aqui ela se apresenta ao núcleo.
@Component
public class DefinicaoAllGenda implements DefinicaoDaAplicacao {

    @Override
    public String nome() {
        return "AllGenda";
    }

    // ponto variável 1: os tipos do AllGenda
    @Override
    public void registrarTipos(RegistroDeTipos registro) {
        registro.registrar(new FabricaDeDisciplina());
        registro.registrar(new FabricaDeAula());
        registro.registrar(new FabricaDeAvaliacao());
    }

    // ponto variável 2: DONO e MEMBRO
    @Override
    public PoliticaDeAcesso politicaDeAcesso() {
        return PoliticaAllGenda.criar();
    }

    // ponto variável 3: o tutor de estudos
    @Override
    public ComportamentoDoAssistente comportamentoDoAssistente() {
        return new AssistenteTutor();
    }
}
