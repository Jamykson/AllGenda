import type { Disciplina, NovaDisciplina, Participante } from '~/types/disciplina'

const CHAVE_PERSONALIZACOES =
  'allgenda-personalizacoes-disciplinas'

interface PersonalizacaoDisciplina {
  nome?: string
  cor?: string
  icone?: string
}

export function useDisciplinas() {
  const { $api } = useNuxtApp()

  const { salvar: salvarRecorrencia, esquecer: esquecerRecorrencia } = useRecorrencias()

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

    for (const chave of [CHAVE_PERSONALIZACOES]) {
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
    const { data: disciplinas } =
      await $api.get<Disciplina[]>('/api/disciplinas')

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
    const { data: criada } = await $api.post<Disciplina>(
      '/api/disciplinas',
      {
        nome: nova.nome,
        descricao: nova.descricao.trim() || null
      }
    )

    // a disciplina já existe; se a recorrência falhar, o dono a define de novo na edição da disciplina
    if (nova.horario && nova.horario.dias.length > 0) {
      try {
        await salvarRecorrencia(criada.id, nova.horario)
      }
      catch (erro) {
        console.error('[disciplinas] falha ao salvar a recorrência:', erro)
      }
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
    await $api.delete(`/api/disciplinas/${disciplinaId}`)

    excluirDadosLocais(disciplinaId)
    esquecerRecorrencia(disciplinaId)
  }

  // só o dono edita; descrição vazia deixa a disciplina sem descrição
  async function atualizarDescricao(disciplinaId: string, descricao: string): Promise<Disciplina> {
    const { data } = await $api.patch<Disciplina>(`/api/disciplinas/${disciplinaId}`, {
      descricao: descricao.trim() || null
    })
    return data
  }

  async function listarParticipantes(disciplinaId: string): Promise<Participante[]> {
    const { data } = await $api.get<Participante[]>(`/api/disciplinas/${disciplinaId}/participantes`)
    return data
  }

  // só o dono remove; a pessoa pode ser convidada de novo, e as anotações do grupo dela continuam
  async function removerParticipante(disciplinaId: string, usuarioId: string): Promise<void> {
    await $api.delete(`/api/disciplinas/${disciplinaId}/participantes/${usuarioId}`)
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
    atualizarDescricao,
    listarParticipantes,
    removerParticipante,
    atualizarCor,
    atualizarIcone,
    atualizarNome
  }
}