<script setup lang="ts">
interface Aula {
  id: string
  data: string
  horario: string
  topico: string | null
  recorrente?: boolean
}

interface HorarioRecorrente {
  dias: number[]
  horaInicio: string
  horaFim: string
  dataInicio: string
  dataFim: string
}

const CHAVE_HORARIOS = 'allgenda-horarios-recorrentes'

const props = defineProps<{
  disciplinaId: string
  cor?: string
}>()

const { $api } = useNuxtApp()
const aulas = ref<Aula[]>([])
const aulaSelecionada = ref<Aula | null>(null)
const carregando = ref(true)
const erro = ref('')

function minutosInicio(horario: string) {
  const correspondencia = /(?:^|\s)(\d{1,2}):(\d{2})/.exec(horario)
  if (!correspondencia) return Number.MAX_SAFE_INTEGER

  return Number(correspondencia[1]) * 60 + Number(correspondencia[2])
}

const aulasOrdenadas = computed(() =>
  [...aulas.value].sort((aulaA, aulaB) =>
    aulaA.data.localeCompare(aulaB.data)
    || minutosInicio(aulaA.horario) - minutosInicio(aulaB.horario)
    || (aulaA.topico || '').localeCompare(aulaB.topico || '', 'pt-BR')
  )
)

function formatarData(data: string) {
  const [ano, mes, dia] = data.split('-').map(Number)
  if (!ano || !mes || !dia) return data

  return new Intl.DateTimeFormat('pt-BR', {
    weekday: 'short',
    day: '2-digit',
    month: 'short',
    year: 'numeric'
  }).format(new Date(ano, mes - 1, dia))
}

function corMaisClara(cor: string) {
  const correspondencia = /^#?([\da-f]{2})([\da-f]{2})([\da-f]{2})$/i.exec(cor)
  if (!correspondencia) return '#e2e8f0'

  const [r, g, b] = correspondencia.slice(1).map(canal => Number.parseInt(canal, 16))
  const fator = 0.72

  const rClaro = Math.round(r + (255 - r) * fator)
  const gClaro = Math.round(g + (255 - g) * fator)
  const bClaro = Math.round(b + (255 - b) * fator)

  return `rgb(${rClaro}, ${gClaro}, ${bClaro})`
}

function aulaJaOcorrida(data: string) {
  const dataAula = new Date(`${data}T00:00:00`)
  const hoje = new Date()
  hoje.setHours(0, 0, 0, 0)

  return dataAula < hoje
}

function estiloAula(aula: Aula) {
  const corBase = props.cor || '#2563eb'
  const jaOcorrida = aulaJaOcorrida(aula.data)

  if (!jaOcorrida) {
    return {
      background: 'transparent',
      borderColor: '#e2e8f0',
      color: '#0f172a'
    }
  }

  const corFundo = corMaisClara(corBase)

  return {
    background: corFundo,
    borderColor: corFundo,
    color: '#0f172a'
  }
}

function lerHorariosRecorrentes(): Record<string, HorarioRecorrente> {
  try {
    const dados = JSON.parse(localStorage.getItem(CHAVE_HORARIOS) || '{}')
    return dados && typeof dados === 'object' && !Array.isArray(dados)
      ? dados as Record<string, HorarioRecorrente>
      : {}
  }
  catch {
    return {}
  }
}

function gerarOcorrencias(horario: HorarioRecorrente): Aula[] {
  if (
    !Array.isArray(horario.dias)
    || horario.dias.length === 0
    || !horario.dataInicio
    || !horario.dataFim
    || !horario.horaInicio
    || !horario.horaFim
  ) {
    return []
  }

  const inicio = new Date(`${horario.dataInicio}T00:00:00`)
  const fim = new Date(`${horario.dataFim}T00:00:00`)
  if (Number.isNaN(inicio.getTime()) || Number.isNaN(fim.getTime()) || inicio > fim) {
    return []
  }

  const ocorrencias: Aula[] = []
  const data = new Date(inicio)

  while (data <= fim) {
    if (horario.dias.includes(data.getDay())) {
      const chaveData = [
        data.getFullYear(),
        String(data.getMonth() + 1).padStart(2, '0'),
        String(data.getDate()).padStart(2, '0')
      ].join('-')

      ocorrencias.push({
        id: `recorrente-${props.disciplinaId}-${chaveData}`,
        data: chaveData,
        horario: `${horario.horaInicio} – ${horario.horaFim}`,
        topico: null,
        recorrente: true
      })
    }

    data.setDate(data.getDate() + 1)
  }

  return ocorrencias
}

async function carregarAulas() {
  carregando.value = true
  erro.value = ''
  aulas.value = []

  try {
    const resposta = await $api.get<Aula[]>('/api/aulas', {
      params: { disciplinaId: props.disciplinaId }
    })
    aulas.value = resposta.data
  }
  catch (error) {
    console.error('[disciplinas] erro ao carregar aulas:', error)
    erro.value = 'Não foi possível carregar as aulas cadastradas.'
  }

  const horario = lerHorariosRecorrentes()[props.disciplinaId]
  if (horario) {
    const ocorrencias = gerarOcorrencias(horario)
    const chavesExistentes = new Set(
      aulas.value.map(aula => `${aula.data}|${minutosInicio(aula.horario)}`)
    )
    aulas.value.push(...ocorrencias.filter(aula =>
      !chavesExistentes.has(`${aula.data}|${minutosInicio(aula.horario)}`)
    ))
  }

  aulas.value = [...aulas.value].sort((aulaA, aulaB) =>
    aulaA.data.localeCompare(aulaB.data)
    || minutosInicio(aulaA.horario) - minutosInicio(aulaB.horario)
    || (aulaA.topico || '').localeCompare(aulaB.topico || '', 'pt-BR')
  )

  carregando.value = false
}

onMounted(carregarAulas)
</script>

<template>
  <section class="lista-aulas" aria-labelledby="titulo-aulas-disciplina">
    <header class="cabecalho-aulas">
      <div>
        <h3 id="titulo-aulas-disciplina">Aulas</h3>
        <p v-if="!carregando && !erro">
          {{ aulas.length }} {{ aulas.length === 1 ? 'aula cadastrada' : 'aulas cadastradas' }}
        </p>
      </div>
    </header>

    <p v-if="carregando" class="estado-aulas" role="status">
      Carregando aulas...
    </p>

    <div v-if="!carregando && erro" class="estado-erro" role="alert">
      <span>{{ erro }} As recorrências locais ainda são exibidas abaixo.</span>
      <button type="button" @click="carregarAulas">Tentar novamente</button>
    </div>

    <p v-if="!carregando && !aulasOrdenadas.length" class="estado-aulas">
      Nenhuma aula cadastrada.
    </p>

    <ol v-else-if="!carregando" class="itens-aulas">
      <li
        v-for="aula in aulasOrdenadas"
        :key="aula.id"
        class="item-aula"
        tabindex="0"
        :style="estiloAula(aula)"
        @click="aulaSelecionada = aula"
        @keydown.enter.prevent="aulaSelecionada = aula"
        @keydown.space.prevent="aulaSelecionada = aula"
      >
        <time class="data-aula" :datetime="aula.data">
          {{ formatarData(aula.data) }}
        </time>
        <div class="detalhes-aula">
          <strong>{{ aula.topico || 'Aula recorrente' }}</strong>
          <span>{{ aula.horario }}</span>
        </div>
      </li>
    </ol>
  </section>

  <AulaModal v-if="aulaSelecionada" :aula="aulaSelecionada" @fechar="aulaSelecionada = null" />
</template>

<style scoped>
.lista-aulas {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #e2e8f0;
}

.cabecalho-aulas {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.cabecalho-aulas h3 {
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
}

.cabecalho-aulas p,
.estado-aulas {
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}

.itens-aulas {
  display: grid;
  gap: 8px;
  max-height: 280px;
  overflow-y: auto;
  padding: 0;
  list-style: none;
}

.item-aula {
  display: grid;
  grid-template-columns: minmax(110px, 150px) minmax(0, 1fr);
  align-items: center;
  gap: 14px;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 7px;
  background: #fff;
  cursor: pointer;
}

.item-aula:focus-visible {
  outline: 2px solid #93c5fd;
  outline-offset: 2px;
}

.data-aula {
  color: #475569;
  font-size: 12px;
  text-transform: capitalize;
}

.detalhes-aula {
  display: flex;
  min-width: 0;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.detalhes-aula strong {
  overflow-wrap: anywhere;
  color: #0f172a;
  font-size: 13px;
}

.detalhes-aula span {
  flex: 0 0 auto;
  color: #64748b;
  font-size: 12px;
}

.estado-erro {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #b91c1c;
  font-size: 13px;
}

.estado-erro button {
  flex: 0 0 auto;
  border: 0;
  background: transparent;
  color: #1d4ed8;
  font-weight: 600;
  cursor: pointer;
}

@media (max-width: 520px) {
  .item-aula {
    grid-template-columns: minmax(0, 1fr);
    gap: 6px;
  }

  .detalhes-aula {
    align-items: flex-start;
    flex-direction: column;
    gap: 3px;
  }
}
</style>