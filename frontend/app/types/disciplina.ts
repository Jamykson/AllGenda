export interface Disciplina {

  id: string

  nome: string

  descricao?: string | null

  cor?: string

  icone?: string

  horarios?: Horario[]

}

// Participante aceito da disciplina (GET /api/disciplinas/{id}/participantes).
// anotacoes conta só as anotações do grupo dele: as privadas não entram na conta.
export interface Participante {
  usuarioId: string
  nome: string
  papel: 'DONO' | 'MEMBRO'
  anotacoes: number
}

export interface NovaDisciplina {
  nome: string
  descricao: string
  cor: string
  icone: string
  horario?: HorarioRecorrente
}

export type FrequenciaRecorrencia = 'SEMANAL' | 'QUINZENAL'

export interface HorarioRecorrente {
  dias: number[]
  horaInicio: string
  horaFim: string
  dataInicio: string
  dataFim: string
  frequencia?: FrequenciaRecorrencia
}



export interface Horario {

  id?: string

  disciplinaId?: string

  diasSemana: string[]

  horaInicio: string

  horaFim: string

  dataInicio: string

  dataFim: string

}