package com.allgenda.framework.assistente;

import java.util.List;

import com.allgenda.framework.assistente.PerguntaAoAssistente.Foco;
import com.allgenda.model.Anotacao;
import com.allgenda.model.Encontro;
import com.allgenda.model.ImagemEncontro;

// O que o assistente pode usar para responder, entregue à aplicação em
// ComportamentoDoAssistente.selecionarContexto. O núcleo monta este contexto já sem as anotações
// privadas. "anotacoes" vêm em ordem cronológica (encontro, depois criação) e "imagens" são as avulsas
// dos encontros (as anexadas vêm junto com a anotação). "destaque" é o encontro da página de onde veio a
// pergunta (pode ser nulo) e "foco" é o item em foco na pergunta atual (pode ser nulo); os dois são
// informativos: o núcleo usa os do pedido, não os devolvidos pela aplicação.
public record ContextoDoAssistente(List<Anotacao> anotacoes, List<ImagemEncontro> imagens,
                                   Encontro destaque, Foco foco) {}
