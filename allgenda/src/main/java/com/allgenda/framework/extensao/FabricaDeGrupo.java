package com.allgenda.framework.extensao;

import com.allgenda.framework.model.Grupo;

// Ponto variável 1 (Factory): a aplicação diz como criar o seu tipo de grupo. A fábrica instancia a
// subclasse e preenche o que é só dela; nome e descrição, já validados, o núcleo preenche depois.
public interface FabricaDeGrupo {

    // nome do tipo no registro (ex.: "DISCIPLINA")
    String tipo();

    Grupo criar(DadosDoGrupo dados);
}
