package com.allgenda.framework.extensao;

import com.allgenda.model.Evento;
import com.allgenda.model.Grupo;

// Ponto variável 1 (Factory): a aplicação diz como criar o seu tipo de evento. A fábrica instancia a
// subclasse e valida e preenche o que é só dela (ex.: converter o subtipo no enum de tipos de evento da
// aplicação, com 400 se for inválido); descrição, data e grupo o núcleo preenche depois.
public interface FabricaDeEvento {

    // nome do tipo no registro (ex.: "AVALIACAO")
    String tipo();

    Evento criar(DadosDoEvento dados, Grupo grupo);
}
