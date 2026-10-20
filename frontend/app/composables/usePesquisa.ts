import type { Ref } from 'vue'
import type { Anotacao, ImagemAnotacao } from '~/types/anotacao'
import type { Disciplina } from '~/types/disciplina'

// "minhas" é filtrado na página, pelo autor; "colegas" usa o endpoint das anotações dos colegas
export type FiltroAutor = 'todos' | 'minhas' | 'colegas'

// por que a disciplina escolhida (ex: vinda de um link) não pode ser pesquisada
export type ProblemaDisciplina = 'sem-acesso' | 'inexistente'

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
  titulo: string // vazio quando a anotação não tem título
  texto: string // vazio quando a anotação só tem título
  imagens: ImagemAnotacao[]
  tags: string[]
  autorId: string
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
    titulo: anotacao.titulo ?? '',
    texto: anotacao.conteudo ?? '',
    imagens: anotacao.imagens,
    tags: anotacao.tags,
    autorId: anotacao.autorId,
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

// Dados da página Pesquisar. A disciplina e o autor escolhidos na página decidem de onde vêm as
// anotações: "Dos colegas" busca o endpoint próprio, e uma disciplina fora da lista do usuário
// (ex: um link de uma disciplina de que ele não participa) é conferida na API, que responde 403 ou 404.
export function usePesquisa(filtros: { disciplinaId: Ref<string>, autor: Ref<FiltroAutor> }) {
  const { listar } = useDisciplinas()
  const { listarPorDisciplina, listarDosColegas, listarTags } = useAnotacoes()
  const { $api } = useNuxtApp()
  const disciplinas = ref<Disciplina[]>([])
  const aulas = ref<AulaPesquisa[]>([])
  const anotacoesPorAula = ref<Record<string, AnotacaoPesquisa[]>>({})
  // anotações dos colegas por disciplina, buscadas só quando o filtro "Dos colegas" pede
  const dosColegasPorDisciplina = ref<Record<string, Anotacao[]>>({})
  const tagsDisponiveis = ref<string[]>([])
  const carregando = ref(false)
  const carregandoColegas = ref(false)
  const erro = ref('')
  const problemaDisciplina = ref<ProblemaDisciplina | null>(null)
  let dadosCarregados = false
  let consultaAtual = 0

  const anotacoesDosColegasPorAula = computed(() =>
    agruparPorAula(Object.values(dosColegasPorDisciplina.value).flat())
  )

  const resultados = computed<ResultadoPesquisa[]>(() => {
    const fonte = filtros.autor.value === 'colegas' ? anotacoesDosColegasPorAula.value : anotacoesPorAula.value
    return aulas.value.flatMap(aula =>
      (fonte[aula.id] || []).map(anotacao => ({ aula, anotacao }))
    )
  })

  async function carregar() {
    carregando.value = true
    erro.value = ''
    dadosCarregados = false
    dosColegasPorDisciplina.value = {}

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
      const tagsDasAnotacoes = new Set(Object.values(anotacoesPorAula.value)
        .flatMap(anotacoes => anotacoes.flatMap(anotacao => anotacao.tags.map(tag => tag.toLowerCase()))))
      // cada disciplina tem as suas tags, então o mesmo nome pode vir mais de uma vez
      const nomesDaApi = [...new Map(tagsDaApi.map(tag => [tag.nome.toLowerCase(), tag.nome])).values()]
      tagsDisponiveis.value = nomesDaApi
        .filter(tag => tagsDasAnotacoes.has(tag.toLowerCase()))
        .sort((tagA, tagB) => tagA.localeCompare(tagB, 'pt-BR'))
      dadosCarregados = true
    }
    catch (error) {
      console.error('[pesquisa] erro ao carregar dados:', error)
      erro.value = 'Não foi possível carregar os dados para pesquisa.'
    }
    finally {
      carregando.value = false
    }

    await prepararFiltros()
  }

  // 403: sem participação aceita; 404 (ou 400, id inválido na URL): a disciplina não existe
  function problemaDoErro(falha: unknown): ProblemaDisciplina | null {
    const status = statusDoErro(falha)
    if (status === 403) return 'sem-acesso'
    if (status === 404 || status === 400) return 'inexistente'
    return null
  }

  // busca o que a disciplina e o autor escolhidos precisam e ainda não foi carregado
  async function prepararFiltros() {
    if (!dadosCarregados) return
    const consulta = ++consultaAtual
    const disciplinaId = filtros.disciplinaId.value
    const colegas = filtros.autor.value === 'colegas'
    problemaDisciplina.value = null
    erro.value = ''

    if (disciplinaId && !disciplinas.value.some(disciplina => disciplina.id === disciplinaId)) {
      await conferirDisciplina(disciplinaId, colegas, consulta)
      return
    }
    if (!colegas) return

    const pendentes = (disciplinaId ? [disciplinaId] : disciplinas.value.map(disciplina => disciplina.id))
      .filter(id => !(id in dosColegasPorDisciplina.value))
    if (!pendentes.length) return

    carregandoColegas.value = true
    try {
      const respostas = await Promise.all(pendentes.map(async (id) => {
        try {
          return [id, await listarDosColegas(id)] as const
        }
        catch (falha) {
          // a participação pode ter acabado depois de a lista de disciplinas ser carregada
          const problema = problemaDoErro(falha)
          if (!problema) throw falha
          if (consulta === consultaAtual && id === disciplinaId) problemaDisciplina.value = problema
          return [id, [] as Anotacao[]] as const
        }
      }))
      dosColegasPorDisciplina.value = { ...dosColegasPorDisciplina.value, ...Object.fromEntries(respostas) }
    }
    catch (falha) {
      console.error('[pesquisa] erro ao carregar anotações dos colegas:', falha)
      if (consulta === consultaAtual) {
        erro.value = mensagemDeErro(falha, 'Não foi possível carregar as anotações dos colegas.')
      }
    }
    finally {
      if (consulta === consultaAtual) carregandoColegas.value = false
    }
  }

  // a disciplina não está entre as do usuário: a API diz se ela não existe ou se falta participação
  async function conferirDisciplina(disciplinaId: string, colegas: boolean, consulta: number) {
    try {
      // se responder, o usuário passou a participar depois de a página carregar; recarregar a
      // página traz a disciplina para a lista
      await (colegas ? listarDosColegas(disciplinaId) : listarPorDisciplina(disciplinaId))
    }
    catch (falha) {
      if (consulta !== consultaAtual) return
      const problema = problemaDoErro(falha)
      if (problema) {
        problemaDisciplina.value = problema
        return
      }
      console.error('[pesquisa] erro ao conferir a disciplina:', falha)
      erro.value = mensagemDeErro(falha, 'Não foi possível carregar as anotações desta disciplina.')
    }
  }

  watch([filtros.disciplinaId, filtros.autor], () => {
    void prepararFiltros()
  })

  return {
    disciplinas,
    tagsDisponiveis,
    carregando,
    carregandoColegas,
    erro,
    problemaDisciplina,
    resultados,
    carregar
  }
}
