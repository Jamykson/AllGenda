// Formatos das respostas da API de anotações (veja docs/api-contrato.md).

export interface ImagemAnotacao {
  id: string
  nome: string
  contentType: string
  tamanho: number
  url: string // caminho do GET que devolve a imagem; exige o token, então é baixada pelo $api
}

// título e conteúdo são opcionais, mas toda anotação tem ao menos um dos dois
export interface Anotacao {
  id: string
  titulo: string | null
  conteudo: string | null
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

// Imagem avulsa da aula, que não pertence a nenhuma anotação.
export interface ImagemAula {
  id: string
  legenda: string | null
  nome: string
  contentType: string
  tamanho: number
  url: string // caminho do GET que devolve a imagem; exige o token, então é baixada pelo $api
  dataCriacao: string
  autorId: string
  autorNome: string
  aulaId: string
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
