import type { Anotacao, ImagemAnotacao } from '~/types/anotacao'
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
  imagens: ImagemAnotacao[]
  tags: string[]
  autorNome: string
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

function paraAnotacaoPesquisa(anotacao: Anotacao): AnotacaoPesquisa {
  return {
    id: anotacao.id,
    texto: anotacao.conteudo,
    imagens: anotacao.imagens,
    tags: anotacao.tags,
    autorNome: anotacao.autorNome
  }
}

function agruparPorAula(anotacoes: Anotacao[]): Record<string, AnotacaoPesquisa[]> {
  const porAula: Record<string, AnotacaoPesquisa[]> = {}
  for (const anotacao of anotacoes) {
    (porAula[anotacao.aulaId] ??= []).push(paraAnotacaoPesquisa(anotacao))
  }
  return porAula
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
  const { listarPorDisciplina, listarTags } = useAnotacoes()
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
        .map(anotacao => ({ aula, anotacao }))
    )
  )

  async function carregar() {
    carregando.value = true
    erro.value = ''

    try {
      disciplinas.value = await listar()

      // aulas e anotações de cada disciplina, e as tags, são buscadas em paralelo
      const [aulasPorDisciplina, anotacoesPorDisciplina, tagsDaApi] = await Promise.all([
        Promise.all(
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
        ),
        Promise.all(disciplinas.value.map(disciplina => listarPorDisciplina(disciplina.id))),
        listarTags()
      ])

      const aulasDaApi = aulasPorDisciplina.flat()
      const horarios = lerRegistroLocal<HorarioRecorrente>(CHAVE_HORARIOS)
      aulas.value = [
        ...aulasDaApi,
        ...gerarAulasRecorrentes(disciplinas.value, aulasDaApi, horarios)
      ]
      anotacoesPorAula.value = agruparPorAula(anotacoesPorDisciplina.flat())

      // só as tags usadas em alguma anotação ajudam a filtrar
      const tagsDasAnotacoes = Object.values(anotacoesPorAula.value)
        .flatMap(anotacoes => anotacoes.flatMap(anotacao => anotacao.tags))
      tagsDisponiveis.value = tagsDaApi
        .filter(tag => tagsDasAnotacoes.includes(tag))
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