import type { Avaliacao, NovaAvaliacao } from '~/types/avaliacao'

export function useAvaliacoes() {
  const { $api } = useNuxtApp()

  async function listar(disciplinaId: string) {
    const { data } = await $api.get<Avaliacao[]>('/api/avaliacoes', { params: { disciplinaId } })
    return data
  }

  async function criar(avaliacao: NovaAvaliacao) {
    const { data } = await $api.post<Avaliacao>('/api/avaliacoes', avaliacao)
    return data
  }

  return { listar, criar }
}
