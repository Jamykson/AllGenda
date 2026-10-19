export interface Convite {
  id: string
  disciplinaId: string
  disciplinaNome: string
  usuarioId: string
  usuarioNome: string
  papel: 'DONO' | 'MEMBRO'
  status: 'PENDENTE' | 'ACEITO' | 'RECUSADO'
}
