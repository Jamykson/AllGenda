import type { HorarioRecorrente } from '~/types/disciplina'

const UM_DIA = 24 * 60 * 60 * 1000

function emDias(chave: string) {
  const [ano, mes, dia] = chave.split('-').map(Number)
  return Date.UTC(ano ?? 0, (mes ?? 1) - 1, dia ?? 1) / UM_DIA
}

/**
 * Diz se a recorrência tem aula na data "AAAA-MM-DD". A data precisa estar no período, cair em um dos
 * dias da semana e, na frequência QUINZENAL, estar numa semana "sim": as semanas (domingo a sábado)
 * são contadas a partir da semana da data de início, e só as de número par têm aula.
 * Esta mesma regra está descrita em docs/api-contrato.md; a aula só passa a existir no backend quando
 * recebe a primeira anotação, imagem ou cancelamento.
 */
export function ocorreNoDia(horario: HorarioRecorrente, chave: string) {
  if (!horario.dataInicio || !horario.dataFim || chave < horario.dataInicio || chave > horario.dataFim) {
    return false
  }
  const dia = emDias(chave)
  // 1970-01-01 foi quinta-feira, então (dias + 4) % 7 dá o dia da semana com domingo = 0
  const diaDaSemana = (((dia + 4) % 7) + 7) % 7
  if (!horario.dias.includes(diaDaSemana)) {
    return false
  }
  if (horario.frequencia !== 'QUINZENAL') {
    return true
  }
  const inicio = emDias(horario.dataInicio)
  const domingoDoInicio = inicio - (((inicio + 4) % 7) + 7) % 7
  return Math.floor((dia - domingoDoInicio) / 7) % 2 === 0
}

export const NOMES_DIAS_CURTOS = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb']
