package com.allgenda.framework.assistente;

import com.allgenda.model.Encontro;
import com.allgenda.model.Grupo;

// Ponto variável 3: o que muda no assistente de uma aplicação para outra. A aplicação define a persona
// (as instruções iniciais) e, se quiser, restringe ou reordena o contexto. Todo o resto é fixo no núcleo
// (AssistenteServiceImpl): acesso, limite de perguntas, filtro de privacidade, limites de texto e de
// imagens, defesa contra injeção de prompt e chamada ao modelo.
public interface ComportamentoDoAssistente {

    // quem o assistente é e como deve responder; destaque é o encontro da página da pergunta (pode ser nulo).
    // O núcleo sempre acrescenta depois as instruções fixas sobre o material.
    String instrucoes(Grupo grupo, Encontro destaque);

    // Padrão: o grupo inteiro. A aplicação pode tirar ou reordenar itens, mas não acrescentar: o núcleo
    // descarta o que não estava no contexto recebido, para que nada privado chegue ao modelo.
    default ContextoDoAssistente selecionarContexto(ContextoDoAssistente contexto) {
        return contexto;
    }
}
