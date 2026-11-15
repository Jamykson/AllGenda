import { isAxiosError } from 'axios'
import type { FrequenciaRecorrencia, HorarioRecorrente } from '~/types/disciplina'

// A recorrência de cada disciplina fica no backend (GET/PUT/DELETE /api/disciplinas/{id}/recorrencia).
// Antes ela morava no localStorage do navegador; ao carregar, o que sobrou lá é enviado uma vez.
const CHAVE_ANTIGA = 'allgenda-horarios-recorrentes'
const CODIGOS_DIAS = ['DOM', 'SEG', 'TER', 'QUA', 'QUI', 'SEX', 'SAB']

interface RecorrenciaApi {
  disciplinaId: string
  diasSemana: string[]
  horaInicio: string
  horaFim: string
  dataInicio: string
  dataFim: string
  frequencia: FrequenciaRecorrencia
}

function doApi(recorrencia: RecorrenciaApi): HorarioRecorrente {
  return {
    dias: recorrencia.diasSemana.map(codigo => CODIGOS_DIAS.indexOf(codigo)).filter(dia => dia >= 0),
    horaInicio: recorrencia.horaInicio,
    horaFim: recorrencia.horaFim,
    dataInicio: recorrencia.dataInicio,
    dataFim: recorrencia.dataFim,
    frequencia: recorrencia.frequencia
  }
}

function paraApi(horario: HorarioRecorrente) {
  return {
    diasSemana: horario.dias.map(dia => CODIGOS_DIAS[dia]),
    horaInicio: horario.horaInicio,
    horaFim: horario.horaFim,
    dataInicio: horario.dataInicio,
    dataFim: horario.dataFim,
    frequencia: horario.frequencia ?? 'SEMANAL'
  }
}

// a busca em andamento fica fora do useState, que só guarda dados simples; só roda no navegador
let buscaEmAndamento: Promise<void> | null = null

export function useRecorrencias() {
  const { $api } = useNuxtApp()

  // compartilhado entre as telas: a agenda, a pesquisa, os cards e as listas leem o mesmo registro
  const recorrencias = useState<Record<string, HorarioRecorrente>>('recorrencias', () => ({}))

  async function importarDoNavegador() {
    if (import.meta.server) return
    let antigas: Record<string, HorarioRecorrente> = {}
    try {
      const lido = JSON.parse(localStorage.getItem(CHAVE_ANTIGA) || '{}')
      antigas = lido && typeof lido === 'object' && !Array.isArray(lido) ? lido : {}
    }
    catch {
      return
    }

    let pendente = false
    for (const [disciplinaId, horario] of Object.entries(antigas)) {
      if (recorrencias.value[disciplinaId] || !Array.isArray(horario?.dias) || !horario.dias.length) continue
      try {
        await salvar(disciplinaId, horario)
      }
      catch (erro) {
        const status = isAxiosError(erro) ? erro.response?.status : undefined
        // 400/403/404: não é possível guardar (não é o dono, disciplina sumiu, dados inválidos); descarta
        if (status === undefined || status >= 500) pendente = true
      }
    }
    if (!pendente) {
      try {
        localStorage.removeItem(CHAVE_ANTIGA)
      }
      catch {
        // sem localStorage, nada a limpar
      }
    }
  }

  async function buscarTodas() {
    try {
      const { data } = await $api.get<RecorrenciaApi[]>('/api/recorrencias')
      recorrencias.value = Object.fromEntries(data.map(item => [item.disciplinaId, doApi(item)]))
      await importarDoNavegador()
    }
    catch (erro) {
      console.error('[recorrencias] falha ao carregar:', erro)
    }
  }

  // várias telas pedem ao mesmo tempo; só uma busca é feita
  function carregar(forcar = false) {
    if (forcar || !buscaEmAndamento) {
      buscaEmAndamento = buscarTodas()
    }
    return buscaEmAndamento
  }

  async function salvar(disciplinaId: string, horario: HorarioRecorrente) {
    const { data } = await $api.put<RecorrenciaApi>(`/api/disciplinas/${disciplinaId}/recorrencia`, paraApi(horario))
    recorrencias.value = { ...recorrencias.value, [disciplinaId]: doApi(data) }
  }

  async function remover(disciplinaId: string) {
    await $api.delete(`/api/disciplinas/${disciplinaId}/recorrencia`)
    esquecer(disciplinaId)
  }

  // só tira do registro em memória (a disciplina foi excluída)
  function esquecer(disciplinaId: string) {
    const { [disciplinaId]: _removida, ...resto } = recorrencias.value
    recorrencias.value = resto
  }

  return { recorrencias, carregar, salvar, remover, esquecer }
}
