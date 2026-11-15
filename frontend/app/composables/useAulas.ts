// Edição de aula (só o dono): PATCH /api/aulas/{id} altera data, horário, tópico e o cancelamento;
// DELETE /api/aulas/{id} apaga a aula com as anotações e imagens dela.
export interface EdicaoAula {
  data?: string
  horario?: string
  topico?: string
  cancelada?: boolean
}

export function useAulas() {
  const { $api } = useNuxtApp()

  async function atualizarAula<T>(aulaId: string, edicao: EdicaoAula) {
    const { data } = await $api.patch<T>(`/api/aulas/${aulaId}`, edicao)
    return data
  }

  async function excluirAula(aulaId: string) {
    await $api.delete(`/api/aulas/${aulaId}`)
  }

  return { atualizarAula, excluirAula }
}
