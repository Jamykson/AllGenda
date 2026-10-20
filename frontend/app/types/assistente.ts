// Conversa com o assistente de IA da página da aula (veja docs/api-contrato.md, "Assistente").

export type PapelMensagem = 'USUARIO' | 'ASSISTENTE'

// a anotação ou a imagem da aula sobre a qual o aluno pergunta; o rótulo é só para a tela
export interface FocoAssistente {
  tipo: 'ANOTACAO' | 'IMAGEM'
  id: string
  rotulo: string
}

export interface MensagemAssistente {
  papel: PapelMensagem
  texto: string
  foco?: FocoAssistente | null
  // a resposta foi parada pelo aluno antes do fim
  interrompida?: boolean
}
