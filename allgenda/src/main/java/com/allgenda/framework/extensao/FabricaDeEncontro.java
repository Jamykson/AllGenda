package com.allgenda.framework.extensao;

import com.allgenda.framework.model.Encontro;
import com.allgenda.framework.model.Grupo;

// Ponto variável 1 (Factory): a aplicação diz como criar o seu tipo de encontro. A fábrica instancia a
// subclasse e preenche o que é só dela; data, horário, tópico e grupo o núcleo preenche depois.
public interface FabricaDeEncontro {

    // nome do tipo no registro (ex.: "AULA")
    String tipo();

    Encontro criar(DadosDoEncontro dados, Grupo grupo);
}
