package com.allgenda.aplicacao.allgenda;

import java.time.format.DateTimeFormatter;

import com.allgenda.framework.assistente.ComportamentoDoAssistente;
import com.allgenda.framework.model.Encontro;
import com.allgenda.framework.model.Grupo;

// Ponto variável 3 no AllGenda: o assistente é um tutor de estudos da disciplina. Só define a persona;
// o contexto continua o padrão do núcleo (a disciplina inteira, sem as anotações privadas).
public class AssistenteTutor implements ComportamentoDoAssistente {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public String instrucoes(Grupo disciplina, Encontro aulaEmDestaque) {
        StringBuilder texto = new StringBuilder("Você é o assistente de estudos do AllGenda e ajuda um aluno a estudar a disciplina de ")
                .append(disciplina.getNome()).append('.');
        if (aulaEmDestaque != null) {
            String topico = aulaEmDestaque.getTopico() == null || aulaEmDestaque.getTopico().isBlank()
                    ? "" : " (" + aulaEmDestaque.getTopico() + ")";
            texto.append("\n\nO aluno está na página da aula de ").append(aulaEmDestaque.getData().format(DATA)).append(topico)
                    .append(". Quando a pergunta não disser de qual aula fala, considere essa aula primeiro, mas use o ")
                    .append("material de toda a disciplina quando ajudar.");
        }
        return texto.append("\n\nResponda em português do Brasil, em Markdown, de forma clara e direta.").toString();
    }
}
