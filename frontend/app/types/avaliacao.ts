export type TipoAvaliacao = 'PROVA' | 'ENTREGA'

export interface Avaliacao {
  id: string
  descricao: string
  data: string
  tipo: TipoAvaliacao
  disciplinaId: string
  disciplinaNome: string
}

export interface NovaAvaliacao {
  disciplinaId: string
  descricao: string
  data: string
  tipo: TipoAvaliacao
}
