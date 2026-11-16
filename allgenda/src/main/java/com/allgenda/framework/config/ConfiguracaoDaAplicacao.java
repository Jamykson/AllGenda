package com.allgenda.framework.config;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.allgenda.framework.acesso.PoliticaDeAcesso;
import com.allgenda.framework.extensao.DefinicaoDaAplicacao;
import com.allgenda.framework.extensao.RegistroDeTipos;

// Liga a aplicação ao núcleo (inversão de controle): encontra a única DefinicaoDaAplicacao do contexto,
// chama registrarTipos na inicialização e expõe a política dela como beans que os services do núcleo usam.
@Configuration
public class ConfiguracaoDaAplicacao {

    private final DefinicaoDaAplicacao definicao;

    public ConfiguracaoDaAplicacao(ObjectProvider<DefinicaoDaAplicacao> definicoes) {
        this.definicao = unica(definicoes.orderedStream().toList());
    }

    // zero ou mais de uma definição é erro de montagem: a inicialização falha com uma mensagem clara
    static DefinicaoDaAplicacao unica(List<DefinicaoDaAplicacao> definicoes) {
        if (definicoes.isEmpty()) {
            throw new IllegalStateException("Nenhuma aplicação definida: o framework precisa de exatamente uma "
                    + "classe que implemente DefinicaoDaAplicacao, anotada com @Component.");
        }
        if (definicoes.size() > 1) {
            List<String> nomes = definicoes.stream().map(DefinicaoDaAplicacao::nome).toList();
            throw new IllegalStateException("Mais de uma aplicação definida " + nomes
                    + ": o framework instancia exatamente uma DefinicaoDaAplicacao.");
        }
        return definicoes.getFirst();
    }

    @Bean
    public RegistroDeTipos registroDeTipos() {
        RegistroDeTipos registro = new RegistroDeTipos();
        definicao.registrarTipos(registro);
        return registro;
    }

    @Bean
    public PoliticaDeAcesso politicaDeAcesso() {
        return definicao.politicaDeAcesso();
    }
}
