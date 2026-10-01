export interface Disciplina {

  id: string

  nome: string

  descricao?: string | null

  cor?: string

  icone?: string

  horarios?: Horario[]

}

export interface NovaDisciplina {
  nome: string
  descricao: string
  cor: string
  icone: string
  horario?: HorarioRecorrente
}

export interface HorarioRecorrente {
  dias: number[]
  horaInicio: string
  horaFim: string
  dataInicio: string
  dataFim: string
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