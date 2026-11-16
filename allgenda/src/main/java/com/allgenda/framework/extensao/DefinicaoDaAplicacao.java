package com.allgenda.framework.extensao;

import com.allgenda.framework.acesso.PoliticaDeAcesso;
import com.allgenda.framework.assistente.ComportamentoDoAssistente;

// Ponto de entrada de uma aplicação construída sobre o framework: uma classe (anotada com @Component)
// que registra os tipos da aplicação e fornece a política de acesso e o comportamento do assistente. O núcleo procura exatamente uma
// definição no contexto Spring e a usa na inicialização (ConfiguracaoDaAplicacao).
public interface DefinicaoDaAplicacao {

    String nome();

    // ponto variável 1: as fábricas dos tipos de grupo, encontro e evento da aplicação
    void registrarTipos(RegistroDeTipos registro);

    // ponto variável 2: papéis e permissões
    PoliticaDeAcesso politicaDeAcesso();

    // ponto variável 3: instruções do assistente e, se quiser, a seleção do contexto
    ComportamentoDoAssistente comportamentoDoAssistente();
}
