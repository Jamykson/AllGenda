import type { Disciplina, HorarioRecorrente, NovaDisciplina } from '~/types/disciplina'

const CHAVE_PERSONALIZACOES =
  'allgenda-personalizacoes-disciplinas'

const CHAVE_HORARIOS =
  'allgenda-horarios-recorrentes'

interface PersonalizacaoDisciplina {
  nome?: string
  cor?: string
  icone?: string
}

export function useDisciplinas() {
  const config = useRuntimeConfig()

  function salvarHorarioDisciplina(
    disciplinaId: string,
    horario: HorarioRecorrente
  ) {
    if (import.meta.server) {
      return
    }

    const horariosSalvos = localStorage.getItem(CHAVE_HORARIOS)
    let horarios: Record<string, HorarioRecorrente> = {}

    if (horariosSalvos) {
      try {
        const parseado = JSON.parse(horariosSalvos)
        if (parseado && typeof parseado === 'object') {
          horarios = parseado as Record<string, HorarioRecorrente>
        }
      }
      catch {
        horarios = {}
      }
    }

    horarios[disciplinaId] = {
      ...horario,
      dias: [...horario.dias]
    }

    localStorage.setItem(CHAVE_HORARIOS, JSON.stringify(horarios))
  }

  function carregarPersonalizacoes(): Record<string, PersonalizacaoDisciplina> {
    if (import.meta.server) {
      return {}
    }

    const salvo = localStorage.getItem(
      CHAVE_PERSONALIZACOES
    )

    if (!salvo) {
      return {}
    }

    try {
      return JSON.parse(salvo)
    }
    catch {
      return {}
    }
  }

  function salvarPersonalizacoes(
    dados: Record<string, PersonalizacaoDisciplina>
  ) {
    if (import.meta.server) {
      return
    }

    localStorage.setItem(
      CHAVE_PERSONALIZACOES,
      JSON.stringify(dados)
    )
  }

  function excluirDadosLocais(disciplinaId: string) {
    if (import.meta.server) {
      return
    }

    for (const chave of [CHAVE_PERSONALIZACOES, CHAVE_HORARIOS]) {
      const salvo = localStorage.getItem(chave)
      if (!salvo) continue

      try {
        const dados = JSON.parse(salvo)
        if (dados && typeof dados === 'object') {
          delete dados[disciplinaId]
          localStorage.setItem(chave, JSON.stringify(dados))
        }
      }
      catch {
        continue
      }
    }
  }

  async function listar(): Promise<Disciplina[]> {
    const disciplinas =
      await $fetch<Disciplina[]>(
        `${config.public.apiBase}/api/disciplinas`
      )

    const personalizacoes =
      carregarPersonalizacoes()

    return disciplinas.map(
      disciplina => ({
        ...disciplina,

        nome:
          personalizacoes[disciplina.id]?.nome
          ?? disciplina.nome,

        cor:
          personalizacoes[disciplina.id]?.cor,

        icone:
          personalizacoes[disciplina.id]?.icone
      })
    )
  }

  async function criar(nova: NovaDisciplina): Promise<Disciplina> {
    const criada = await $fetch<Disciplina>(
      `${config.public.apiBase}/api/disciplinas`,
      {
        method: 'POST',
        body: {
          nome: nova.nome,
          descricao: nova.descricao.trim() || null
        }
      }
    )

    if (nova.horario && nova.horario.dias.length > 0) {
      salvarHorarioDisciplina(criada.id, nova.horario)
    }

    const personalizacoes = carregarPersonalizacoes()
    personalizacoes[criada.id] = {
      ...personalizacoes[criada.id],
      cor: nova.cor,
      icone: nova.icone
    }
    salvarPersonalizacoes(personalizacoes)

    return {
      ...criada,
      cor: nova.cor,
      icone: nova.icone
    }
  }

  async function excluir(disciplinaId: string): Promise<void> {
    await $fetch(
      `${config.public.apiBase}/api/disciplinas/${disciplinaId}`,
      { method: 'DELETE' }
    )

    excluirDadosLocais(disciplinaId)
  }

  function atualizarCor(
    disciplinaId: string,
    cor: string
  ) {
    const personalizacoes =
      carregarPersonalizacoes()

    personalizacoes[disciplinaId] = {
      ...personalizacoes[disciplinaId],
      cor
    }

    salvarPersonalizacoes(
      personalizacoes
    )
  }

  function atualizarIcone(
    disciplinaId: string,
    icone: string
  ) {
    const personalizacoes =
      carregarPersonalizacoes()

    personalizacoes[disciplinaId] = {
      ...personalizacoes[disciplinaId],
      icone
    }

    salvarPersonalizacoes(
      personalizacoes
    )
  }

  function atualizarNome(
    disciplinaId: string,
    nome: string
  ) {
    const personalizacoes =
      carregarPersonalizacoes()

    personalizacoes[disciplinaId] = {
      ...personalizacoes[disciplinaId],
      nome
    }

    salvarPersonalizacoes(
      personalizacoes
    )
  }

  return {
    listar,
    criar,
    excluir,
    atualizarCor,
    atualizarIcone,
    atualizarNome
  }
}