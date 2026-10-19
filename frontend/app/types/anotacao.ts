// Formatos das respostas da API de anotações (veja docs/api-contrato.md).

export interface ImagemAnotacao {
  id: string
  nome: string
  contentType: string
  tamanho: number
  url: string // caminho do GET que devolve a imagem; exige o token, então é baixada pelo $api
}

export interface Anotacao {
  id: string
  conteudo: string
  dataCriacao: string
  autorId: string
  autorNome: string
  tags: string[]
  disciplinaNome: string
  aulaId: string
  aulaData: string
  aulaTopico: string | null
  imagens: ImagemAnotacao[]
}

export interface Tag {
  id: string
  nome: string
}

// Aula exibida na área de anotações. Aulas recorrentes são geradas no navegador a partir dos
// horários da disciplina e ainda não existem no backend: são cadastradas ao receber a primeira anotação.
export interface AulaDaAnotacao {
  id: string
  data: string
  horario: string
  topico?: string | null
  recorrente?: boolean
  disciplinaId?: string
}
