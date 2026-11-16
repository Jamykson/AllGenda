package com.allgenda.aplicacao.allgenda;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.allgenda.framework.acesso.PoliticaDeAcesso;

// Provisório: liga a política do AllGenda ao núcleo até existir a DefinicaoDaAplicacao.
@Configuration
public class ConfiguracaoAllGenda {

    @Bean
    PoliticaDeAcesso politicaDeAcesso() {
        return PoliticaAllGenda.criar();
    }
}
