// Data de uma aula ("2026-10-21") por extenso curto, ex.: "qua., 21 de out. de 2026".
// A data é montada no fuso local para não voltar um dia por causa do UTC.
export function formatarDataDaAula(data: string) {
  const [ano, mes, dia] = data.split('-').map(Number)
  if (!ano || !mes || !dia) return data

  return new Intl.DateTimeFormat('pt-BR', {
    weekday: 'short',
    day: '2-digit',
    month: 'short',
    year: 'numeric'
  }).format(new Date(ano, mes - 1, dia))
}
