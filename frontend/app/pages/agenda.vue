<template>
  <div class="app">
    <!-- SIDEBAR -->
    <aside class="sidebar">
      <div class="brand">
        <h1>All<span>Genda</span></h1>
        <p>Organização acadêmica</p>
      </div>

      <nav class="menu">
        <NuxtLink to="/agenda" class="menu-item ativo">
          <span>▣</span>
          Agenda
        </NuxtLink>

        <NuxtLink to="/disciplinas" class="menu-item">
          <span>▤</span>
          Disciplinas
        </NuxtLink>
      </nav>

      <div class="sidebar-footer">
        <button class="menu-item botao-sair" @click="sair">
          <span>↪</span>
          Sair
        </button>
      </div>
    </aside>

    <!-- CONTEÚDO PRINCIPAL -->
    <main class="conteudo">
      <!-- HEADER -->
      <header class="topbar">
        <div class="navegacao-data">
          <button class="btn-hoje" @click="irParaHoje">
            Hoje
          </button>

          <button class="btn-seta" @click="anterior">
            ‹
          </button>

          <button class="btn-seta" @click="proximo">
            ›
          </button>

          <h2>{{ tituloPeriodo }}</h2>
        </div>

        <div class="seletor">
          <button
            :class="{ selecionado: visualizacao === 'semana' }"
            @click="visualizacao = 'semana'"
          >
            Semana
          </button>

          <button
            :class="{ selecionado: visualizacao === 'mes' }"
            @click="visualizacao = 'mes'"
          >
            Mês
          </button>
        </div>
      </header>

      <!-- ÁREA DO CALENDÁRIO -->
      <section class="area-calendario">
        <div v-if="carregando" class="mensagem">
          Carregando agenda...
        </div>

        <div v-else-if="erro" class="erro">
          {{ erro }}
        </div>

        <!-- CALENDÁRIO MENSAL -->
        <div
          v-else-if="visualizacao === 'mes'"
          class="calendario"
        >
          <div class="cabecalho-dias">
            <div
              v-for="nome in nomesDias"
              :key="nome"
              class="nome-dia"
            >
              {{ nome }}
            </div>
          </div>

          <div class="grade-mes">
            <div
              v-for="dia in diasCalendario"
              :key="dia.chave"
              class="celula-dia celula-clicavel"
              :class="{
                foraMes: !dia.mesAtual,
                diaHoje: dia.hoje
              }"
            >
              <div class="numero-dia">
                <span :class="{ hoje: dia.hoje }">
                  {{ dia.numero }}
                </span>
              </div>

              <div class="eventos-dia">
                <template
                  v-for="aula in aulasDoDia(dia.chave)"
                  :key="aula.id"
                >
                  <div
                    v-if="aula.recorrente"
                    class="evento evento-recorrente"
                    :style="estiloCorAula(aula)"
                    title="Horário recorrente da disciplina"
                  >
                    <strong>
                      <span v-if="iconeAula(aula)" class="evento-icone">
                        {{ iconeAula(aula) }}
                      </span>
                      {{ aula.disciplinaNome }}
                    </strong>

                    <small>
                      {{ aula.horario }}
                    </small>
                  </div>

                  <NuxtLink
                    v-else
                    :to="`/aulas/${aula.id}`"
                    class="evento"
                    :style="estiloCorAula(aula)"
                  >
                    <strong>
                      <span v-if="iconeAula(aula)" class="evento-icone">
                        {{ iconeAula(aula) }}
                      </span>
                      {{ aula.disciplinaNome }}
                    </strong>

                    <small>
                      {{ aula.horario }}
                    </small>
                  </NuxtLink>
                </template>
              </div>
            </div>
          </div>
        </div>

        <!-- CALENDÁRIO SEMANAL -->
        <div
          v-else
          class="calendario-semana"
        >
          <!-- CABEÇALHO DA SEMANA -->
          <div class="semana-cabecalho">
            <div class="coluna-horas"></div>

            <div
              v-for="dia in diasSemanaAtual"
              :key="dia.chave"
              class="semana-dia"
            >
              <span>{{ dia.nome }}</span>

              <strong :class="{ hojeSemana: dia.hoje }">
                {{ dia.numero }}
              </strong>
            </div>
          </div>

          <!-- GRADE DA SEMANA -->
          <div
            class="grade-semana"
            :style="{ height: `${alturaSemana}px` }"
          >
            <!-- HORÁRIOS -->
            <div class="horarios">
              <div
                v-for="hora in horas"
                :key="hora"
                class="hora"
                :style="{
                  top: `${(hora - horaInicial) * alturaHora + alturaHora / 2}px`
                }"
              >
                {{ formatarHora(hora) }}
              </div>
            </div>

            <!-- DIAS -->
            <div
              v-for="dia in diasSemanaAtual"
              :key="dia.chave"
              class="coluna-dia-semana"
            >
              <!-- LINHAS DE HORÁRIO -->
              <div
                v-for="hora in horas"
                :key="hora"
                class="linha-hora linha-hora-clicavel"
                :style="{
                  top: `${(hora - horaInicial) * alturaHora}px`
                }"
              ></div>

              <!-- AULAS -->
              <template
                v-for="aula in aulasDoDia(dia.chave)"
                :key="aula.id"
              >
                <div
                  v-if="aula.recorrente"
                  class="evento-semana evento-recorrente"
                  :style="{
                    ...estiloAulaSemana(aula),
                    ...estiloCorAula(aula)
                  }"
                  title="Horário recorrente da disciplina"
                >
                  <strong>
                    <span v-if="iconeAula(aula)" class="evento-icone">
                      {{ iconeAula(aula) }}
                    </span>
                    {{ aula.disciplinaNome }}
                  </strong>

                  <small>
                    {{ aula.horario }}
                  </small>
                </div>

                <NuxtLink
                  v-else
                  :to="`/aulas/${aula.id}`"
                  class="evento-semana"
                  :style="{
                    ...estiloAulaSemana(aula),
                    ...estiloCorAula(aula)
                  }"
                >
                  <strong>
                    <span v-if="iconeAula(aula)" class="evento-icone">
                      {{ iconeAula(aula) }}
                    </span>
                    {{ aula.disciplinaNome }}
                  </strong>

                  <small>
                    {{ aula.horario }}
                  </small>

                  <span v-if="aula.topico">
                    {{ aula.topico }}
                  </span>
                </NuxtLink>
              </template>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
interface Disciplina {
  id: string
  nome: string
}

interface HorarioRecorrente {
  dias: number[]
  horaInicio: string
  horaFim: string
  dataInicio: string
  dataFim: string
}

interface PersonalizacaoDisciplina {
  nome?: string
  icone: string
  cor: string
}

interface Aula {
  id: string
  data: string
  horario: string
  topico?: string
  disciplinaId: string
  disciplinaNome: string
  recorrente?: boolean
  cor?: string
  icone?: string
}

const { $api } = useNuxtApp()

const visualizacao = ref<'mes' | 'semana'>('mes')

const dataAtual = ref(new Date())

const disciplinas = ref<Disciplina[]>([])
const aulas = ref<Aula[]>([])

const horariosRecorrentes =
  ref<Record<string, HorarioRecorrente>>({})

const personalizacoes =
  ref<Record<string, PersonalizacaoDisciplina>>({})

const CHAVE_HORARIOS =
  'allgenda-horarios-recorrentes'

const CHAVE_PERSONALIZACOES =
  'allgenda-personalizacoes-disciplinas'

const carregando = ref(true)
const erro = ref('')

const nomesDias = [
  'Dom',
  'Seg',
  'Ter',
  'Qua',
  'Qui',
  'Sex',
  'Sáb'
]

/*
  A semana vai de 07:00 até 00:00.
  Cada hora ocupa 64px.
*/
const horaInicial = 7
const horaFinal = 24
const alturaHora = 64

const horas = Array.from(
  { length: horaFinal - horaInicial },
  (_, i) => horaInicial + i
)

const alturaSemana =
  (horaFinal - horaInicial) * alturaHora

function chaveData(data: Date) {
  const ano = data.getFullYear()

  const mes = String(
    data.getMonth() + 1
  ).padStart(2, '0')

  const dia = String(
    data.getDate()
  ).padStart(2, '0')

  return `${ano}-${mes}-${dia}`
}

function mesmaData(a: Date, b: Date) {
  return chaveData(a) === chaveData(b)
}

function inicioDaSemana(data: Date) {
  const inicio = new Date(data)

  inicio.setHours(0, 0, 0, 0)

  inicio.setDate(
    inicio.getDate() - inicio.getDay()
  )

  return inicio
}

const tituloPeriodo = computed(() => {
  if (visualizacao.value === 'mes') {
    return new Intl.DateTimeFormat(
      'pt-BR',
      {
        month: 'long',
        year: 'numeric'
      }
    ).format(dataAtual.value)
  }

  const inicio =
    inicioDaSemana(dataAtual.value)

  const fim =
    new Date(inicio)

  fim.setDate(
    fim.getDate() + 6
  )

  return `${inicio.toLocaleDateString(
    'pt-BR',
    {
      day: '2-digit',
      month: 'short'
    }
  )} - ${fim.toLocaleDateString(
    'pt-BR',
    {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    }
  )}`
})

const diasCalendario = computed(() => {
  const ano =
    dataAtual.value.getFullYear()

  const mes =
    dataAtual.value.getMonth()

  const primeiroDia =
    new Date(ano, mes, 1)

  const inicio =
    new Date(primeiroDia)

  inicio.setDate(
    primeiroDia.getDate() -
      primeiroDia.getDay()
  )

  return Array.from(
    { length: 42 },
    (_, indice) => {
      const data =
        new Date(inicio)

      data.setDate(
        inicio.getDate() + indice
      )

      return {
        chave: chaveData(data),

        numero:
          data.getDate(),

        mesAtual:
          data.getMonth() === mes,

        hoje:
          mesmaData(
            data,
            new Date()
          )
      }
    }
  )
})

const diasSemanaAtual = computed(() => {
  const inicio =
    inicioDaSemana(
      dataAtual.value
    )

  return Array.from(
    { length: 7 },
    (_, indice) => {
      const data =
        new Date(inicio)

      data.setDate(
        inicio.getDate() + indice
      )

      return {
        chave: chaveData(data),

        numero:
          data.getDate(),

        nome:
          nomesDias[indice],

        hoje:
          mesmaData(
            data,
            new Date()
          )
      }
    }
  )
})

function aulasDoDia(chave: string) {
  return aulas.value
    .filter(
      aula =>
        aula.data === chave
    )
    .sort(
      (a, b) =>
        a.horario.localeCompare(
          b.horario
        )
    )
}

function carregarDadosLocais() {
  if (!import.meta.client) {
    return
  }

  try {
    horariosRecorrentes.value =
      JSON.parse(
        localStorage.getItem(
          CHAVE_HORARIOS
        ) || '{}'
      )
  } catch {
    horariosRecorrentes.value = {}
  }

  try {
    personalizacoes.value =
      JSON.parse(
        localStorage.getItem(
          CHAVE_PERSONALIZACOES
        ) || '{}'
      )
  } catch {
    personalizacoes.value = {}
  }
}

function criarDataLocal(
  texto: string
) {
  const [ano, mes, dia] =
    texto
      .split('-')
      .map(Number)

  return new Date(
    ano,
    mes - 1,
    dia
  )
}

function nomeDisciplina(
  disciplina: Disciplina
) {
  return (
    personalizacoes.value[
      disciplina.id
    ]?.nome?.trim() ||
    disciplina.nome
  )
}

function gerarAulasRecorrentes() {
  const resultado: Aula[] = []

  for (
    const disciplina
    of disciplinas.value
  ) {
    const horario =
      horariosRecorrentes.value[
        disciplina.id
      ]

    if (!horario) {
      continue
    }

    const inicio =
      criarDataLocal(
        horario.dataInicio
      )

    const fim =
      criarDataLocal(
        horario.dataFim
      )

    if (
      Number.isNaN(inicio.getTime()) ||
      Number.isNaN(fim.getTime())
    ) {
      continue
    }

    const atual =
      new Date(inicio)

    while (
      atual.getTime() <=
      fim.getTime()
    ) {
      const diaSemana =
        atual.getDay()

      if (
        horario.dias.includes(
          diaSemana
        )
      ) {
        const personalizacao =
          personalizacoes.value[
            disciplina.id
          ]

        const data =
          chaveData(atual)

        resultado.push({
          id:
            `recorrente-${disciplina.id}-${data}`,

          data,

          horario:
            `${horario.horaInicio} - ${horario.horaFim}`,

          topico: '',

          disciplinaId:
            disciplina.id,

          disciplinaNome:
            nomeDisciplina(disciplina),

          recorrente:
            true,

          cor:
            personalizacao?.cor ||
            '#2563eb',

          icone:
            personalizacao?.icone ||
            disciplina.nome
              .trim()
              .charAt(0)
              .toUpperCase()
        })
      }

      atual.setDate(
        atual.getDate() + 1
      )
    }
  }

  return resultado
}

function corAula(
  aula: Aula
) {
  return (
    aula.cor ||
    personalizacoes.value[
      aula.disciplinaId
    ]?.cor ||
    '#2563eb'
  )
}

function iconeAula(
  aula: Aula
) {
  return (
    aula.icone ||
    personalizacoes.value[
      aula.disciplinaId
    ]?.icone ||
    ''
  )
}

function hexParaRgba(
  hex: string,
  alpha: number
) {
  const valor =
    hex.replace('#', '')

  if (valor.length !== 6) {
    return `rgba(37, 99, 235, ${alpha})`
  }

  const r =
    parseInt(
      valor.substring(0, 2),
      16
    )

  const g =
    parseInt(
      valor.substring(2, 4),
      16
    )

  const b =
    parseInt(
      valor.substring(4, 6),
      16
    )

  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

function estiloCorAula(
  aula: Aula
) {
  const cor =
    corAula(aula)

  return {
    borderColor: cor,
    color: cor,
    background:
      hexParaRgba(
        cor,
        0.13
      )
  }
}

function minutosInicio(
  horario: string
) {
  const inicio =
    horario
      .split('-')[0]
      ?.trim() || ''

  const [hora, minuto] =
    inicio
      .split(':')
      .map(Number)

  if (Number.isNaN(hora)) {
    return horaInicial * 60
  }

  return (
    hora * 60 +
    (minuto || 0)
  )
}

function minutosFim(
  horario: string
) {
  const fim =
    horario
      .split('-')[1]
      ?.trim()

  if (!fim) {
    return (
      minutosInicio(horario) + 60
    )
  }

  const [hora, minuto] =
    fim
      .split(':')
      .map(Number)

  if (Number.isNaN(hora)) {
    return (
      minutosInicio(horario) + 60
    )
  }

  return (
    hora * 60 +
    (minuto || 0)
  )
}

function estiloAulaSemana(
  aula: Aula
) {
  const inicio =
    minutosInicio(
      aula.horario
    )

  const fim =
    minutosFim(
      aula.horario
    )

  const inicioGrade =
    horaInicial * 60

  const top =
    ((inicio - inicioGrade) / 60) *
    alturaHora

  const height =
    Math.max(
      ((fim - inicio) / 60) *
        alturaHora,
      42
    )

  return {
    top:
      `${Math.max(top, 0)}px`,

    height:
      `${height}px`
  }
}

function formatarHora(
  hora: number
) {
  return `${String(hora).padStart(
    2,
    '0'
  )}:00`
}

function anterior() {
  const nova =
    new Date(
      dataAtual.value
    )

  if (
    visualizacao.value ===
    'mes'
  ) {
    nova.setMonth(
      nova.getMonth() - 1
    )
  } else {
    nova.setDate(
      nova.getDate() - 7
    )
  }

  dataAtual.value = nova
}

function proximo() {
  const nova =
    new Date(
      dataAtual.value
    )

  if (
    visualizacao.value ===
    'mes'
  ) {
    nova.setMonth(
      nova.getMonth() + 1
    )
  } else {
    nova.setDate(
      nova.getDate() + 7
    )
  }

  dataAtual.value = nova
}

function irParaHoje() {
  dataAtual.value =
    new Date()
}

async function carregarAgenda() {
  carregando.value = true
  erro.value = ''

  carregarDadosLocais()

  try {
    const resposta =
      await $api.get(
        '/api/disciplinas'
      )

    disciplinas.value =
      resposta.data

    const respostas =
      await Promise.all(
        disciplinas.value.map(
          async disciplina => {
            const resultado =
              await $api.get(
                '/api/aulas',
                {
                  params: {
                    disciplinaId:
                      disciplina.id
                  }
                }
              )

            const personalizacao =
              personalizacoes.value[
                disciplina.id
              ]

            return resultado.data.map(
              (aula: any) => ({
                ...aula,

                disciplinaId:
                  disciplina.id,

                disciplinaNome:
                  nomeDisciplina(disciplina),

                cor:
                  personalizacao?.cor ||
                  '#2563eb',

                icone:
                  personalizacao?.icone ||
                  ''
              })
            )
          }
        )
      )

    const aulasBackend: Aula[] =
      respostas.flat()

    const aulasRecorrentes =
      gerarAulasRecorrentes()
        .filter(
          recorrente =>
            !aulasBackend.some(
              real =>
                real.disciplinaId ===
                  recorrente.disciplinaId &&
                real.data ===
                  recorrente.data
            )
        )

    aulas.value = [
      ...aulasBackend,
      ...aulasRecorrentes
    ]
  } catch (e) {
    console.error(
      '[agenda] erro:',
      e
    )

    erro.value =
      'Não foi possível carregar a agenda.'
  } finally {
    carregando.value = false
  }
}

async function sair() {
  await navigateTo('/')
}

onMounted(
  carregarAgenda
)
</script>

<style scoped>
* {
  box-sizing: border-box;
}

/* APP */

.app {
  display: flex;

  width: 100%;
  min-height: 100vh;

  background: #f8fafc;
  color: #0f172a;
}

/* SIDEBAR */

.sidebar {
  width: 230px;
  min-width: 230px;

  height: 100vh;

  position: sticky;
  top: 0;

  display: flex;
  flex-direction: column;

  background: #0f172a;
  color: white;
}

.brand {
  padding: 26px 22px;

  border-bottom:
    1px solid #1e293b;
}

.brand h1 {
  margin: 0;

  font-size: 25px;
  font-weight: 700;
}

.brand h1 span {
  color: #60a5fa;
}

.brand p {
  margin-top: 4px;
  margin-bottom: 0;

  font-size: 12px;

  color: #94a3b8;
}

/* MENU */

.menu {
  flex: 1;

  padding: 16px 12px;
}

.menu-item {
  width: 100%;

  display: flex;
  align-items: center;

  gap: 12px;

  padding: 12px 14px;

  border-radius: 8px;

  color: #cbd5e1;

  font-size: 14px;

  text-decoration: none;

  transition: 0.2s;
}

.menu-item:hover {
  background: #1e293b;
  color: white;
}

.menu-item.ativo {
  background: #2563eb;
  color: white;
}

.sidebar-footer {
  padding: 14px 12px;

  border-top:
    1px solid #1e293b;
}

.botao-sair {
  border: none;

  background: transparent;

  cursor: pointer;

  text-align: left;
}

/* CONTEÚDO */

.conteudo {
  flex: 1;

  min-width: 0;

  height: 100vh;

  display: flex;
  flex-direction: column;

  overflow: hidden;
}

/* HEADER */

.topbar {
  min-height: 72px;

  display: flex;
  align-items: center;
  justify-content: space-between;

  gap: 20px;

  padding: 14px 24px;

  flex-shrink: 0;

  background: white;

  border-bottom:
    1px solid #e2e8f0;
}

.navegacao-data {
  display: flex;
  align-items: center;

  gap: 8px;

  min-width: 0;
}

.navegacao-data h2 {
  margin:
    0 0 0 12px;

  font-size: 20px;

  font-weight: 600;

  text-transform: capitalize;

  white-space: nowrap;
}

.btn-hoje {
  padding: 9px 16px;

  border:
    1px solid #cbd5e1;

  border-radius: 8px;

  background: white;

  cursor: pointer;

  font-weight: 500;

  color: #0f172a;
}

.btn-hoje:hover {
  background: #f8fafc;
}

.btn-seta {
  width: 38px;
  height: 38px;

  border: none;

  border-radius: 50%;

  background: transparent;

  cursor: pointer;

  font-size: 25px;

  color: #475569;
}

.btn-seta:hover {
  background: #f1f5f9;
}

/* SELETOR MÊS / SEMANA */

.linha-hora-clicavel {
  height: 64px;

  cursor: pointer;

  transition:
    background-color 0.15s ease;

  z-index: 1;
}

.linha-hora-clicavel:hover {
  background: rgba(219, 234, 254, 0.45);
}

.seletor {
  display: flex;

  padding: 3px;

  border:
    1px solid #cbd5e1;

  border-radius: 9px;

  background: #f1f5f9;

  flex-shrink: 0;
}

.seletor button {
  padding: 8px 16px;

  border: none;

  border-radius: 6px;

  background: transparent;

  cursor: pointer;

  color: #475569;
}

.seletor button.selecionado {
  background: #2563eb;
  color: white;

  box-shadow:
    0 1px 3px
    rgba(0, 0, 0, 0.15);
}

/* ÁREA DO CALENDÁRIO */

.area-calendario {
  flex: 1;

  min-height: 0;

  padding:
    0 20px 32px 20px;

  overflow-x: auto;
  overflow-y: auto;

  scrollbar-gutter: stable;

  background: #f8fafc;
}

/* CALENDÁRIO MENSAL */

.celula-clicavel {
  cursor: pointer;
  transition:
    background-color 0.18s ease,
    box-shadow 0.18s ease;
}

.celula-clicavel:hover {
  background: #eff6ff;
  box-shadow: inset 0 0 0 2px #bfdbfe;
}

.celula-clicavel:hover .numero-dia span:not(.hoje) {
  background: #dbeafe;
  color: #1d4ed8;
}

.coluna-dia-semana {
  cursor: pointer;

  transition:
    background-color 0.18s ease;
}

.coluna-dia-semana:hover {
  background: #f8fbff;
}

.calendario {
  width: 100%;

  margin-top: 20px;

  background: white;

  border:
    1px solid #e2e8f0;

  border-radius: 12px;

  overflow: hidden;

  box-shadow:
    0 1px 3px
    rgba(15, 23, 42, 0.08);
}

/* CABEÇALHO DIAS */

.cabecalho-dias {
  display: grid;

  grid-template-columns:
    repeat(7, 1fr);

  background: #f8fafc;

  border-bottom:
    1px solid #e2e8f0;
}

.nome-dia {
  padding: 12px;

  text-align: center;

  font-size: 12px;

  font-weight: 600;

  color: #64748b;

  text-transform: uppercase;
}

/* GRADE MÊS */

.grade-mes {
  display: grid;

  grid-template-columns:
    repeat(
      7,
      minmax(0, 1fr)
    );
}

.celula-dia {
  min-height: 135px;

  padding: 8px;

  border-right:
    1px solid #e2e8f0;

  border-bottom:
    1px solid #e2e8f0;

  background: white;
}

.celula-dia:nth-child(7n) {
  border-right: none;
}

.celula-dia.foraMes {
  background: #f8fafc;
}

.celula-dia.diaHoje {
  background: #eff6ff;
}

.numero-dia {
  display: flex;

  justify-content: flex-end;

  margin-bottom: 6px;
}

.numero-dia span {
  width: 28px;
  height: 28px;

  display: flex;

  align-items: center;
  justify-content: center;

  border-radius: 50%;

  font-size: 13px;

  color: #334155;
}

.numero-dia span.hoje {
  background: #2563eb;

  color: white;

  font-weight: 600;
}

/* EVENTOS DO MÊS */

.eventos-dia {
  display: flex;

  flex-direction: column;

  gap: 4px;
}

.evento {
  display: block;

  padding: 6px 7px;

  border-left:
    4px solid;

  border-radius: 6px;

  text-decoration: none;

  font-size: 11px;

  overflow: hidden;
}

.evento strong {
  display: block;

  white-space: nowrap;

  overflow: hidden;

  text-overflow: ellipsis;
}

.evento small {
  display: block;

  margin-top: 2px;
}

.evento-icone {
  margin-right: 4px;
}

.evento-recorrente {
  cursor: default;
}

/* CORES DOS EVENTOS */

.evento-azul {
  background: #dbeafe;

  border-color: #2563eb;

  color: #1e3a8a;
}

.evento-ciano {
  background: #cffafe;

  border-color: #0891b2;

  color: #164e63;
}

.evento-indigo {
  background: #e0e7ff;

  border-color: #4f46e5;

  color: #312e81;
}

.evento-royal {
  background: #dbeafe;

  border-color: #1d4ed8;

  color: #172554;
}

/* CALENDÁRIO SEMANAL */

.calendario-semana {
  width: 100%;

  min-width: 900px;

  margin-top: 0;

  background: white;

  border:
    1px solid #e2e8f0;

  border-radius:
    0 0 12px 12px;

  overflow: visible;
}

/* CABEÇALHO DA SEMANA */

.semana-cabecalho {
  display: grid;

  grid-template-columns:
    70px repeat(
      7,
      minmax(
        120px,
        1fr
      )
    );

  position: sticky;

  top: 0;

  z-index: 20;

  background: #f8fafc;

  border-bottom:
    1px solid #e2e8f0;

  box-shadow:
    0 1px 0
    rgba(15, 23, 42, 0.05);
}

.coluna-horas {
  border-right:
    1px solid #e2e8f0;
}

.semana-dia {
  height: 72px;

  display: flex;

  flex-direction: column;

  align-items: center;
  justify-content: center;

  border-right:
    1px solid #e2e8f0;
}

.semana-dia:last-child {
  border-right: none;
}

.semana-dia span {
  font-size: 11px;

  font-weight: 600;

  color: #64748b;

  text-transform: uppercase;
}

.semana-dia strong {
  width: 36px;
  height: 36px;

  margin-top: 4px;

  display: flex;

  align-items: center;
  justify-content: center;

  border-radius: 50%;

  font-size: 18px;
}

.semana-dia strong.hojeSemana {
  background: #2563eb;

  color: white;
}

/* GRADE SEMANAL */

.grade-semana {
  display: grid;

  grid-template-columns:
    70px repeat(
      7,
      minmax(
        120px,
        1fr
      )
    );

  position: relative;
}

/* COLUNA DE HORÁRIOS */

.horarios {
  position: relative;

  border-right:
    1px solid #e2e8f0;

  background: #ffffff;
}

.hora {
  position: absolute;

  right: 10px;

  transform:
    translateY(-50%);

  font-size: 11px;

  font-weight: 500;

  color: #94a3b8;

  white-space: nowrap;
}

/* COLUNAS DOS DIAS */

.coluna-dia-semana {
  position: relative;

  border-right:
    1px solid #e2e8f0;

  background: white;
}

.coluna-dia-semana:last-child {
  border-right: none;
}

/* LINHAS DAS HORAS */

.linha-hora {
  position: absolute;

  left: 0;
  right: 0;

  border-top:
    1px solid #e2e8f0;
}

/* EVENTOS DA SEMANA */

.evento-semana {
  position: absolute;

  left: 4px;
  right: 4px;

  z-index: 2;

  padding: 7px;

  border-left:
    4px solid;

  border-radius: 7px;

  overflow: hidden;

  text-decoration: none;

  font-size: 11px;

  transition:
    filter 0.15s,
    transform 0.15s;
}

.evento-semana:hover {
  filter: brightness(0.96);

  transform: translateY(-2px);

  box-shadow:
    0 4px 10px
    rgba(15, 23, 42, 0.12);

  cursor: pointer;
}

.evento {
  cursor: pointer;

  transition:
    transform 0.15s ease,
    filter 0.15s ease,
    box-shadow 0.15s ease;
}

.evento:hover {
  transform: translateY(-1px);

  filter: brightness(0.97);

  box-shadow:
    0 3px 8px
    rgba(15, 23, 42, 0.12);
}

.evento-semana strong {
  display: block;

  font-size: 11px;
}

.evento-semana small {
  display: block;

  margin-top: 2px;
}

.evento-semana span {
  display: block;

  margin-top: 3px;

  white-space: nowrap;

  overflow: hidden;

  text-overflow: ellipsis;
}

/* ESTADOS */

.mensagem {
  margin-top: 20px;

  padding: 50px;

  text-align: center;

  background: white;

  border-radius: 12px;

  color: #64748b;
}

.erro {
  margin-top: 20px;

  padding: 16px;

  border:
    1px solid #fecaca;

  border-radius: 8px;

  background: #fef2f2;

  color: #dc2626;
}

/* RESPONSIVIDADE */

@media (max-width: 900px) {
  .sidebar {
    width: 180px;
    min-width: 180px;
  }

  .topbar {
    align-items:
      flex-start;

    flex-direction:
      column;
  }

  .navegacao-data {
    flex-wrap: wrap;
  }

  .navegacao-data h2 {
    white-space: normal;
  }

  .area-calendario {
    overflow-x: auto;
    overflow-y: auto;
  }

  .calendario {
    min-width: 800px;
  }

  .calendario-semana {
    min-width: 900px;
  }
}
</style>