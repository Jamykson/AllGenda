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
  posicao: number // ordem na galeria da aula, a mesma para todos
}

export interface Tag {
  id: string
  nome: string
}

// Aula exibida na página da aula. Aulas recorrentes são geradas no navegador a partir dos horários
// da disciplina e ainda não existem no backend: são cadastradas ao receber a primeira anotação ou imagem.
export interface AulaDaAnotacao {
  id: string
  data: string
  horario: string
  topico?: string | null
  recorrente?: boolean
  disciplinaId?: string
}

// Aula como a página da aula a mostra: a do backend (GET /api/aulas/{id}) ou uma recorrente montada pela URL.
export interface AulaDaPagina extends AulaDaAnotacao {
  disciplinaId: string
  disciplinaNome: string
}

// Painel de imagens da página da aula: altura ajustável, recolhido (só a barra) ou ocupando tudo.
export type ModoPainel = 'normal' | 'recolhido' | 'maximizado'
