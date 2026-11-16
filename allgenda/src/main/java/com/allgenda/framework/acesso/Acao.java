package com.allgenda.framework.acesso;

// O que uma pessoa pode tentar fazer num grupo. A PoliticaDeAcesso da aplicação diz quais papéis podem
// cada ação; o núcleo pergunta a ela antes de agir (AcessoAoGrupo).
public enum Acao {
    VER_CONTEUDO,
    CRIAR_ENCONTRO,
    EDITAR_ENCONTRO,
    CANCELAR_ENCONTRO,
    EXCLUIR_ENCONTRO,
    CRIAR_EVENTO,
    CRIAR_ANOTACAO, // também cobre as tags e as imagens dos encontros
    CONVIDAR,
    REMOVER_MEMBRO,
    EDITAR_GRUPO,
    EXCLUIR_GRUPO,
    DEFINIR_RECORRENCIA,
    USAR_ASSISTENTE
}
