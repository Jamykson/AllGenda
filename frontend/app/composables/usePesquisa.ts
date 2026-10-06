import type { Disciplina } from '~/types/disciplina'

export interface AulaPesquisa {
  id: string
  disciplinaId: string
  disciplinaNome: string
  disciplinaCor?: string
  disciplinaIcone?: string
  data: string
  horario: string
  topico?: string | null
  recorrente?: boolean
}

export interface AnotacaoPesquisa {
  id: string
  texto: string
  imagens: string[]
  videos: string[]
  tags: string[]
}

export interface ResultadoPesquisa {
  aula: AulaPesquisa
  anotacao: AnotacaoPesquisa
}

interface HorarioRecorrente {
  dias: number[]
  horaInicio: string
  horaFim: string
  dataInicio: string
  dataFim: string
}

interface AulaApi {
  id: string
  data: string
  horario: string
  topico?: string | null
}

const CHAVE_ANOTACOES = 'allgenda-anotacoes-aulas'
const CHAVE_TAGS = 'allgenda-tags-disponiveis'
const CHAVE_HORARIOS = 'allgenda-horarios-recorrentes'

function lerRegistroLocal<T>(chave: string): Record<string, T> {
  try {
    const registro = JSON.parse(localStorage.getItem(chave) || '{}')
    return registro && typeof registro === 'object' && !Array.isArray(registro)
      ? registro as Record<string, T>
      : {}
  }
  catch {
    return {}
  }
}

function lerListaTags(): string[] {
  try {
    const tags = JSON.parse(localStorage.getItem(CHAVE_TAGS) || '[]')
    return Array.isArray(tags)
      ? tags.filter((tag): tag is string => typeof tag === 'string')
      : []
  }
  catch {
    return []
  }
}

function normalizarAnotacao(
  anotacao: Partial<AnotacaoPesquisa> | null | undefined,
  idFallback: string
): AnotacaoPesquisa {
  return {
    id: typeof anotacao?.id === 'string' ? anotacao.id : idFallback,
    texto: typeof anotacao?.texto === 'string' ? anotacao.texto : '',
    imagens: Array.isArray(anotacao?.imagens) ? anotacao.imagens : [],
    videos: Array.isArray(anotacao?.videos) ? anotacao.videos : [],
    tags: Array.isArray(anotacao?.tags)
      ? anotacao.tags.filter((tag): tag is string => typeof tag === 'string')
      : []
  }
}

function anotacaoTemConteudo(anotacao: AnotacaoPesquisa) {
  return Boolean(
    anotacao.texto.trim()
    || anotacao.tags.length
    || anotacao.imagens.length
    || anotacao.videos.length
  )
}

function lerAnotacoes(): Record<string, AnotacaoPesquisa[]> {
  const salvas = lerRegistroLocal<{
    anotacoes?: Partial<AnotacaoPesquisa>[]
    texto?: string
    imagens?: string[]
    videos?: string[]
    tags?: string[]
  }>(CHAVE_ANOTACOES)

  return Object.fromEntries(
    Object.entries(salvas).map(([aulaId, registro]) => {
      const anotacoes = Array.isArray(registro?.anotacoes)
        ? registro.anotacoes.map((anotacao, indice) =>
            normalizarAnotacao(anotacao, `${aulaId}-${indice}`)
          )
        : registro && ['texto', 'tags', 'imagens', 'videos'].some(campo => campo in registro)
          ? [normalizarAnotacao(registro, `${aulaId}-legada`)]
          : []

      return [aulaId, anotacoes]
    })
  )
}

function chaveData(data: Date) {
  const ano = data.getFullYear()
  const mes = String(data.getMonth() + 1).padStart(2, '0')
  const dia = String(data.getDate()).padStart(2, '0')
  return `${ano}-${mes}-${dia}`
}

function gerarAulasRecorrentes(
  disciplinas: Disciplina[],
  aulasDaApi: AulaPesquisa[],
  horarios: Record<string, HorarioRecorrente>
): AulaPesquisa[] {
  return Object.entries(horarios).flatMap(([disciplinaId, horario]) => {
    const disciplina = disciplinas.find(item => item.id === disciplinaId)
    if (
      !disciplina
      || !Array.isArray(horario.dias)
      || !horario.dias.length
      || !horario.dataInicio
      || !horario.dataFim
      || !horario.horaInicio
      || !horario.horaFim
    ) {
      return []
    }

    const inicio = new Date(`${horario.dataInicio}T00:00:00`)
    const fim = new Date(`${horario.dataFim}T00:00:00`)
    if (Number.isNaN(inicio.getTime()) || Number.isNaN(fim.getTime()) || inicio > fim) {
      return []
    }

    const ocorrencias: AulaPesquisa[] = []
    const data = new Date(inicio)
    const horarioFormatado = `${horario.horaInicio} – ${horario.horaFim}`

    while (data <= fim) {
      const dataAula = chaveData(data)
      const duplicada = aulasDaApi.some(aula =>
        aula.disciplinaId === disciplinaId
        && aula.data === dataAula
        && aula.horario === horarioFormatado
      )

      if (horario.dias.includes(data.getDay()) && !duplicada) {
        ocorrencias.push({
          id: `recorrente-${disciplinaId}-${dataAula}`,
          disciplinaId,
          disciplinaNome: disciplina.nome,
          disciplinaCor: disciplina.cor,
          disciplinaIcone: disciplina.icone,
          data: dataAula,
          horario: horarioFormatado,
          topico: null,
          recorrente: true
        })
      }

      data.setDate(data.getDate() + 1)
    }

    return ocorrencias
  })
}

export function usePesquisa() {
  const { listar } = useDisciplinas()
  const { $api } = useNuxtApp()
  const disciplinas = ref<Disciplina[]>([])
  const aulas = ref<AulaPesquisa[]>([])
  const anotacoesPorAula = ref<Record<string, AnotacaoPesquisa[]>>({})
  const tagsDisponiveis = ref<string[]>([])
  const carregando = ref(false)
  const erro = ref('')

  const resultados = computed<ResultadoPesquisa[]>(() =>
    aulas.value.flatMap(aula =>
      (anotacoesPorAula.value[aula.id] || [])
        .filter(anotacaoTemConteudo)
        .map(anotacao => ({ aula, anotacao }))
    )
  )

  async function carregar() {
    carregando.value = true
    erro.value = ''

    try {
      disciplinas.value = await listar()

      const aulasPorDisciplina = await Promise.all(
        disciplinas.value.map(async (disciplina) => {
          const resposta = await $api.get<AulaApi[]>('/api/aulas', {
            params: { disciplinaId: disciplina.id }
          })

          return resposta.data.map(aula => ({
            ...aula,
            disciplinaId: disciplina.id,
            disciplinaNome: disciplina.nome,
            disciplinaCor: disciplina.cor,
            disciplinaIcone: disciplina.icone
          }))
        })
      )

      const aulasDaApi = aulasPorDisciplina.flat()
      const horarios = lerRegistroLocal<HorarioRecorrente>(CHAVE_HORARIOS)
      aulas.value = [
        ...aulasDaApi,
        ...gerarAulasRecorrentes(disciplinas.value, aulasDaApi, horarios)
      ]
      anotacoesPorAula.value = lerAnotacoes()

      const listaTagsLocais = lerListaTags()
      const tagsDasAnotacoes = Object.values(anotacoesPorAula.value)
        .flatMap(anotacoes => anotacoes.flatMap(anotacao => anotacao.tags))

      tagsDisponiveis.value = [...new Set([...listaTagsLocais, ...tagsDasAnotacoes])]
        .sort((tagA, tagB) => tagA.localeCompare(tagB, 'pt-BR'))
    }
    catch (error) {
      console.error('[pesquisa] erro ao carregar dados:', error)
      erro.value = 'Não foi possível carregar os dados para pesquisa.'
    }
    finally {
      carregando.value = false
    }
  }

  return {
    disciplinas,
    tagsDisponiveis,
    carregando,
    erro,
    resultados,
    carregar
  }
}