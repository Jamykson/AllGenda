package com.allgenda.framework.extensao;

import java.util.LinkedHashMap;
import java.util.Map;

import com.allgenda.exception.RequisicaoInvalidaException;

// Ponto variável 1 (Factory com registro): guarda, pelo nome do tipo, a fábrica de cada tipo de grupo,
// encontro e evento da aplicação. A aplicação registra as fábricas na inicialização
// (DefinicaoDaAplicacao.registrarTipos); os services do núcleo só pedem a fábrica pelo nome e nunca
// conhecem as subclasses (inversão de controle).
public class RegistroDeTipos {

    private final Map<String, FabricaDeGrupo> grupos = new LinkedHashMap<>();
    private final Map<String, FabricaDeEncontro> encontros = new LinkedHashMap<>();
    private final Map<String, FabricaDeEvento> eventos = new LinkedHashMap<>();

    public void registrar(FabricaDeGrupo fabrica) {
        guardar(grupos, fabrica.tipo(), fabrica, "grupo");
    }

    public void registrar(FabricaDeEncontro fabrica) {
        guardar(encontros, fabrica.tipo(), fabrica, "encontro");
    }

    public void registrar(FabricaDeEvento fabrica) {
        guardar(eventos, fabrica.tipo(), fabrica, "evento");
    }

    // tipo desconhecido: 400
    public FabricaDeGrupo fabricaDeGrupo(String tipo) {
        return buscar(grupos, tipo, "grupo");
    }

    public FabricaDeEncontro fabricaDeEncontro(String tipo) {
        return buscar(encontros, tipo, "encontro");
    }

    public FabricaDeEvento fabricaDeEvento(String tipo) {
        return buscar(eventos, tipo, "evento");
    }

    // dois registros com o mesmo nome são um erro da aplicação, e aparecem logo na inicialização
    private static <F> void guardar(Map<String, F> fabricas, String tipo, F fabrica, String deQue) {
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("A fábrica de " + deQue + " precisa informar o nome do tipo.");
        }
        if (fabricas.putIfAbsent(tipo, fabrica) != null) {
            throw new IllegalStateException("Já existe uma fábrica de " + deQue + " para o tipo " + tipo + ".");
        }
    }

    private static <F> F buscar(Map<String, F> fabricas, String tipo, String deQue) {
        F fabrica = tipo == null ? null : fabricas.get(tipo);
        if (fabrica == null) {
            throw new RequisicaoInvalidaException("Tipo de " + deQue + " desconhecido: " + tipo + ".");
        }
        return fabrica;
    }
}
