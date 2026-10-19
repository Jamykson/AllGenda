import type { Convite } from '~/types/convite'

export function useConvites() {
  const { $api } = useNuxtApp()

  async function listarPendentes() {
    const { data } = await $api.get<Convite[]>('/api/convites', { params: { status: 'PENDENTE' } })
    return data
  }

  async function enviar(disciplinaId: string, email: string) {
    const { data } = await $api.post<Convite>(`/api/disciplinas/${disciplinaId}/convites`, { email })
    return data
  }

  async function responder(id: string, resposta: 'aceitar' | 'recusar') {
    const { data } = await $api.patch<Convite>(`/api/convites/${id}/${resposta}`)
    return data
  }

  return { listarPendentes, enviar, responder }
}
