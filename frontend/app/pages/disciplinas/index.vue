<template>
  <div class="app">
    <!-- SIDEBAR -->
    <aside class="sidebar">
      <div class="brand">
        <h1>All<span>Genda</span></h1>
        <p>Organização acadêmica</p>
      </div>

      <nav class="menu">
        <NuxtLink
          to="/agenda"
          class="menu-item"
        >
          <span>▣</span>
          Agenda
        </NuxtLink>

        <NuxtLink
          to="/disciplinas"
          class="menu-item ativo"
        >
          <span>▤</span>
          Disciplinas
        </NuxtLink>
      </nav>

      <div class="sidebar-footer">
        <button
          type="button"
          class="menu-item botao-sair"
          @click="sair"
        >
          <span>↪</span>
          Sair
        </button>
      </div>
    </aside>

    <!-- CONTEÚDO PRINCIPAL -->
    <main class="conteudo">
      <header class="topbar">
        <div>
          <h2>Disciplinas</h2>

          <p class="subtitulo">
            Gerencie suas disciplinas e horários
          </p>
        </div>

        <button
          type="button"
          class="btn-nova"
          @click="abrirFormulario"
        >
          + Nova disciplina
        </button>
      </header>

      <section class="pagina">
        <!-- NOVA DISCIPLINA -->
        <div
          v-if="mostrarFormulario"
          class="formulario-card"
        >
          <div class="formulario-topo">
            <div>
              <h3>Nova disciplina</h3>

              <p>
                Informe o nome da disciplina.
              </p>
            </div>

            <button
              type="button"
              class="btn-fechar"
              @click="fecharFormulario"
            >
              ×
            </button>
          </div>

          <form
            class="formulario"
            @submit.prevent="criarDisciplina"
          >
            <div class="campo">
              <label for="nome-disciplina">
                Nome da disciplina
              </label>

              <input
                id="nome-disciplina"
                v-model="nome"
                type="text"
                placeholder="Ex.: Projeto Detalhado de Software"
              />
            </div>

            <p
              v-if="erroFormulario"
              class="erro-formulario"
            >
              {{ erroFormulario }}
            </p>

            <div class="acoes-formulario">
              <button
                type="button"
                class="btn-cancelar"
                @click="fecharFormulario"
              >
                Cancelar
              </button>

              <button
                type="submit"
                class="btn-salvar"
                :disabled="salvando"
              >
                {{
                  salvando
                    ? 'Salvando...'
                    : 'Criar disciplina'
                }}
              </button>
            </div>
          </form>
        </div>

        <!-- MENSAGEM -->
        <div
          v-if="mensagem"
          class="mensagem-sucesso"
        >
          {{ mensagem }}
        </div>

        <!-- CARREGAMENTO -->
        <div
          v-if="carregando"
          class="estado"
        >
          Carregando disciplinas...
        </div>

        <!-- ERRO -->
        <div
          v-else-if="erro"
          class="estado erro"
        >
          {{ erro }}
        </div>

        <!-- SEM DISCIPLINAS -->
        <div
          v-else-if="disciplinas.length === 0"
          class="vazio"
        >
          <div class="icone-vazio">
            ▤
          </div>

          <h3>
            Nenhuma disciplina cadastrada
          </h3>

          <p>
            Crie sua primeira disciplina para começar
            a organizar seus horários.
          </p>

          <button
            type="button"
            class="btn-nova"
            @click="abrirFormulario"
          >
            + Nova disciplina
          </button>
        </div>

        <!-- GRID -->
        <div
          v-else
          class="grid-disciplinas"
        >
          <article
            v-for="disciplina in disciplinas"
            :key="disciplina.id"
            class="disciplina-card"
          >
            <button
              type="button"
              class="disciplina-conteudo"
              @click="abrirDisciplina(disciplina)"
            >
              <div class="icone-disciplina">
                {{ primeiraLetra(disciplina.nome) }}
              </div>

              <div class="disciplina-info">
                <h3>
                  {{ disciplina.nome }}
                </h3>

                <p
                  v-if="temHorario(disciplina.id)"
                  class="resumo-horario"
                >
                  {{ resumoHorario(disciplina.id) }}
                </p>

                <p v-else>
                  Clique para configurar o horário
                </p>
              </div>
            </button>

            <div class="card-footer">
              <button
                type="button"
                class="btn-acessar"
                @click="abrirDisciplina(disciplina)"
              >
                Visualizar
              </button>

              <button
                type="button"
                class="btn-excluir"
                @click="pedirExclusao(disciplina)"
              >
                Excluir
              </button>
            </div>
          </article>
        </div>
      </section>
    </main>

    <!-- MODAL DA DISCIPLINA -->
    <div
      v-if="disciplinaAberta"
      class="modal-fundo"
      @click.self="fecharDisciplina"
    >
      <div class="modal-disciplina">
        <!-- TOPO -->
        <div class="modal-disciplina-topo">
          <div class="titulo-disciplina-modal">
            <div class="icone-disciplina">
              {{ primeiraLetra(disciplinaAberta.nome) }}
            </div>

            <div>
              <h2>
                {{ disciplinaAberta.nome }}
              </h2>

              <p>
                Configure o horário recorrente
              </p>
            </div>
          </div>

          <button
            type="button"
            class="btn-fechar"
            @click="fecharDisciplina"
          >
            ×
          </button>
        </div>

        <!-- CORPO -->
        <div class="modal-disciplina-corpo">
          <section class="horario-recorrente">
            <div class="horario-topo">
              <div>
                <h3>
                  Horário recorrente
                </h3>

                <p>
                  Defina os dias e o período em que
                  esta disciplina acontece.
                </p>
              </div>

              <button
                v-if="
                  !mostrarFormularioHorario &&
                  !horarioAtual
                "
                type="button"
                class="btn-nova-aula"
                @click="abrirFormularioHorario"
              >
                + Adicionar horário
              </button>

              <button
                v-else-if="
                  !mostrarFormularioHorario &&
                  horarioAtual
                "
                type="button"
                class="btn-editar-horario"
                @click="editarHorario"
              >
                Editar
              </button>
            </div>

            <!-- HORÁRIO SALVO -->
            <div
              v-if="
                horarioAtual &&
                !mostrarFormularioHorario
              "
              class="horario-salvo"
            >
              <div class="horario-principal">
                <div class="dias-salvos">
                  <span
                    v-for="dia in horarioAtual.dias"
                    :key="dia"
                    class="dia-chip-salvo"
                  >
                    {{ nomeDia(dia) }}
                  </span>
                </div>

                <strong>
                  {{ horarioAtual.horaInicio }}
                  —
                  {{ horarioAtual.horaFim }}
                </strong>
              </div>

              <div class="periodo-horario">
                <span>
                  {{ formatarData(horarioAtual.dataInicio) }}
                </span>

                <span>→</span>

                <span>
                  {{ formatarData(horarioAtual.dataFim) }}
                </span>
              </div>

              <div class="horario-observacao">
                Este horário está salvo apenas temporariamente no front.
              </div>
            </div>

            <!-- FORMULÁRIO -->
            <form
              v-if="mostrarFormularioHorario"
              class="form-horario"
              @submit.prevent="salvarHorario"
            >
              <div class="campo">
                <label>
                  Dias da semana
                </label>

                <div class="dias-semana">
                  <button
                    v-for="dia in diasSemana"
                    :key="dia.valor"
                    type="button"
                    class="dia-semana"
                    :class="{
                      selecionado:
                        formularioHorario.dias.includes(
                          dia.valor
                        )
                    }"
                    @click="alternarDia(dia.valor)"
                  >
                    {{ dia.label }}
                  </button>
                </div>
              </div>

              <div class="linha-campos">
                <div class="campo">
                  <label for="hora-inicio">
                    Horário inicial
                  </label>

                  <input
                    id="hora-inicio"
                    v-model="formularioHorario.horaInicio"
                    type="time"
                  />
                </div>

                <div class="campo">
                  <label for="hora-fim">
                    Horário final
                  </label>

                  <input
                    id="hora-fim"
                    v-model="formularioHorario.horaFim"
                    type="time"
                  />
                </div>
              </div>

              <div class="linha-campos">
                <div class="campo">
                  <label for="data-inicio">
                    Início das aulas
                  </label>

                  <input
                    id="data-inicio"
                    v-model="formularioHorario.dataInicio"
                    type="date"
                  />
                </div>

                <div class="campo">
                  <label for="data-fim">
                    Fim das aulas
                  </label>

                  <input
                    id="data-fim"
                    v-model="formularioHorario.dataFim"
                    type="date"
                  />
                </div>
              </div>

              <p
                v-if="erroHorario"
                class="erro-formulario"
              >
                {{ erroHorario }}
              </p>

              <div class="acoes-formulario">
                <button
                  type="button"
                  class="btn-cancelar"
                  @click="cancelarHorario"
                >
                  Cancelar
                </button>

                <button
                  type="submit"
                  class="btn-salvar"
                >
                  Salvar horário
                </button>
              </div>
            </form>

            <!-- ESTADO SEM HORÁRIO -->
            <div
              v-if="
                !horarioAtual &&
                !mostrarFormularioHorario
              "
              class="sem-horario"
            >
              <div class="sem-horario-icone">
                ◷
              </div>

              <h4>
                Nenhum horário configurado
              </h4>

              <p>
                Adicione os dias da semana, horário
                e período desta disciplina.
              </p>
            </div>
          </section>
        </div>
      </div>
    </div>

    <!-- MODAL EXCLUSÃO -->
    <div
      v-if="disciplinaParaExcluir"
      class="modal-fundo"
      @click.self="cancelarExclusao"
    >
      <div class="modal-exclusao">
        <div class="modal-icone">
          !
        </div>

        <h3>
          Excluir disciplina?
        </h3>

        <p>
          Você está prestes a excluir
          <strong>
            {{ disciplinaParaExcluir.nome }}
          </strong>.
        </p>

        <p class="aviso">
          Essa ação não poderá ser desfeita.
        </p>

        <div class="modal-acoes">
          <button
            type="button"
            class="btn-cancelar"
            @click="cancelarExclusao"
          >
            Cancelar
          </button>

          <button
            type="button"
            class="btn-confirmar-exclusao"
            :disabled="excluindo"
            @click="excluirDisciplina"
          >
            {{
              excluindo
                ? 'Excluindo...'
                : 'Excluir'
            }}
          </button>
        </div>
      </div>
    </div>
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

const { $api } = useNuxtApp()

/* DISCIPLINAS */

const disciplinas =
  ref<Disciplina[]>([])

const nome =
  ref('')

const carregando =
  ref(true)

const salvando =
  ref(false)

const excluindo =
  ref(false)

const erro =
  ref('')

const erroFormulario =
  ref('')

const mensagem =
  ref('')

const mostrarFormulario =
  ref(false)

const disciplinaParaExcluir =
  ref<Disciplina | null>(null)

/* MODAL */

const disciplinaAberta =
  ref<Disciplina | null>(null)

/* HORÁRIOS RECORRENTES - SOMENTE FRONT */

const mostrarFormularioHorario =
  ref(false)

const erroHorario =
  ref('')

const horariosRecorrentes =
  ref<
    Record<
      string,
      HorarioRecorrente
    >
  >({})

const formularioHorario =
  reactive({
    dias: [] as number[],
    horaInicio: '',
    horaFim: '',
    dataInicio: '',
    dataFim: ''
  })

const diasSemana = [
  {
    valor: 1,
    label: 'Seg'
  },
  {
    valor: 2,
    label: 'Ter'
  },
  {
    valor: 3,
    label: 'Qua'
  },
  {
    valor: 4,
    label: 'Qui'
  },
  {
    valor: 5,
    label: 'Sex'
  },
  {
    valor: 6,
    label: 'Sáb'
  }
]

const horarioAtual =
  computed(() => {
    if (!disciplinaAberta.value) {
      return null
    }

    return (
      horariosRecorrentes.value[
        disciplinaAberta.value.id
      ] || null
    )
  })

/* FUNÇÕES DISCIPLINAS */

function primeiraLetra(
  nome: string
) {
  return nome
    .trim()
    .charAt(0)
    .toUpperCase()
}

function abrirFormulario() {
  mostrarFormulario.value = true

  erroFormulario.value = ''

  mensagem.value = ''
}

function fecharFormulario() {
  mostrarFormulario.value = false

  nome.value = ''

  erroFormulario.value = ''
}

async function carregarDisciplinas() {
  carregando.value = true

  erro.value = ''

  try {
    const resposta =
      await $api.get(
        '/api/disciplinas'
      )

    disciplinas.value =
      resposta.data
  } catch (e) {
    console.error(
      '[disciplinas] erro ao carregar:',
      e
    )

    erro.value =
      'Não foi possível carregar as disciplinas.'
  } finally {
    carregando.value = false
  }
}

async function criarDisciplina() {
  erroFormulario.value = ''

  mensagem.value = ''

  if (!nome.value.trim()) {
    erroFormulario.value =
      'Informe o nome da disciplina.'

    return
  }

  const jaExiste =
    disciplinas.value.some(
      disciplina =>
        disciplina.nome
          .trim()
          .toLowerCase() ===
        nome.value
          .trim()
          .toLowerCase()
    )

  if (jaExiste) {
    erroFormulario.value =
      'Essa disciplina já está cadastrada.'

    return
  }

  salvando.value = true

  try {
    const resposta =
      await $api.post(
        '/api/disciplinas',
        {
          nome:
            nome.value.trim()
        }
      )

    disciplinas.value.push(
      resposta.data
    )

    disciplinas.value.sort(
      (a, b) =>
        a.nome.localeCompare(
          b.nome,
          'pt-BR'
        )
    )

    mensagem.value =
      'Disciplina criada com sucesso.'

    fecharFormulario()
  } catch (e) {
    console.error(
      '[disciplinas] erro ao criar:',
      e
    )

    erroFormulario.value =
      'Não foi possível criar a disciplina.'
  } finally {
    salvando.value = false
  }
}

/* MODAL DISCIPLINA */

function abrirDisciplina(
  disciplina: Disciplina
) {
  disciplinaAberta.value =
    disciplina

  mostrarFormularioHorario.value =
    false

  erroHorario.value = ''

  limparFormularioHorario()
}

function fecharDisciplina() {
  disciplinaAberta.value =
    null

  mostrarFormularioHorario.value =
    false

  erroHorario.value = ''

  limparFormularioHorario()
}

/* HORÁRIOS */

function limparFormularioHorario() {
  formularioHorario.dias = []

  formularioHorario.horaInicio = ''

  formularioHorario.horaFim = ''

  formularioHorario.dataInicio = ''

  formularioHorario.dataFim = ''

  erroHorario.value = ''
}

function abrirFormularioHorario() {
  limparFormularioHorario()

  mostrarFormularioHorario.value =
    true
}

function alternarDia(
  dia: number
) {
  if (
    formularioHorario.dias.includes(
      dia
    )
  ) {
    formularioHorario.dias =
      formularioHorario.dias.filter(
        item =>
          item !== dia
      )

    return
  }

  formularioHorario.dias.push(
    dia
  )

  formularioHorario.dias.sort(
    (a, b) =>
      a - b
  )
}

function nomeDia(
  dia: number
) {
  const encontrado =
    diasSemana.find(
      item =>
        item.valor === dia
    )

  return encontrado?.label || ''
}

function salvarHorario() {
  erroHorario.value = ''

  if (!disciplinaAberta.value) {
    return
  }

  if (
    formularioHorario.dias.length === 0
  ) {
    erroHorario.value =
      'Selecione pelo menos um dia da semana.'

    return
  }

  if (
    !formularioHorario.horaInicio ||
    !formularioHorario.horaFim
  ) {
    erroHorario.value =
      'Informe o horário inicial e final.'

    return
  }

  if (
    formularioHorario.horaFim <=
    formularioHorario.horaInicio
  ) {
    erroHorario.value =
      'O horário final deve ser depois do horário inicial.'

    return
  }

  if (
    !formularioHorario.dataInicio ||
    !formularioHorario.dataFim
  ) {
    erroHorario.value =
      'Informe o início e o fim do período.'

    return
  }

  if (
    formularioHorario.dataFim <
    formularioHorario.dataInicio
  ) {
    erroHorario.value =
      'A data final deve ser depois da data inicial.'

    return
  }

  horariosRecorrentes.value[
    disciplinaAberta.value.id
  ] = {
    dias: [
      ...formularioHorario.dias
    ],

    horaInicio:
      formularioHorario.horaInicio,

    horaFim:
      formularioHorario.horaFim,

    dataInicio:
      formularioHorario.dataInicio,

    dataFim:
      formularioHorario.dataFim
  }

  mostrarFormularioHorario.value =
    false
}

function editarHorario() {
  if (!horarioAtual.value) {
    return
  }

  formularioHorario.dias = [
    ...horarioAtual.value.dias
  ]

  formularioHorario.horaInicio =
    horarioAtual.value.horaInicio

  formularioHorario.horaFim =
    horarioAtual.value.horaFim

  formularioHorario.dataInicio =
    horarioAtual.value.dataInicio

  formularioHorario.dataFim =
    horarioAtual.value.dataFim

  erroHorario.value = ''

  mostrarFormularioHorario.value =
    true
}

function cancelarHorario() {
  mostrarFormularioHorario.value =
    false

  limparFormularioHorario()
}

function temHorario(
  disciplinaId: string
) {
  return Boolean(
    horariosRecorrentes.value[
      disciplinaId
    ]
  )
}

function resumoHorario(
  disciplinaId: string
) {
  const horario =
    horariosRecorrentes.value[
      disciplinaId
    ]

  if (!horario) {
    return ''
  }

  const dias =
    horario.dias
      .map(nomeDia)
      .join(' • ')

  return `${dias} · ${horario.horaInicio}–${horario.horaFim}`
}

function formatarData(
  data: string
) {
  if (!data) {
    return ''
  }

  const [
    ano,
    mes,
    dia
  ] =
    data.split('-')

  return `${dia}/${mes}/${ano}`
}

/* EXCLUSÃO */

function pedirExclusao(
  disciplina: Disciplina
) {
  disciplinaParaExcluir.value =
    disciplina

  mensagem.value = ''
}

function cancelarExclusao() {
  disciplinaParaExcluir.value =
    null
}

async function excluirDisciplina() {
  if (
    !disciplinaParaExcluir.value
  ) {
    return
  }

  excluindo.value = true

  const disciplina =
    disciplinaParaExcluir.value

  try {
    await $api.delete(
      `/api/disciplinas/${disciplina.id}`
    )

    disciplinas.value =
      disciplinas.value.filter(
        item =>
          item.id !==
          disciplina.id
      )

    delete horariosRecorrentes.value[
      disciplina.id
    ]

    disciplinaParaExcluir.value =
      null

    if (
      disciplinaAberta.value?.id ===
      disciplina.id
    ) {
      fecharDisciplina()
    }

    mensagem.value =
      'Disciplina excluída com sucesso.'
  } catch (e) {
    console.error(
      '[disciplinas] erro ao excluir:',
      e
    )

    alert(
      'Não foi possível excluir a disciplina.'
    )
  } finally {
    excluindo.value = false
  }
}

async function sair() {
  await navigateTo('/')
}

onMounted(
  carregarDisciplinas
)
</script>

<style scoped>
* {
  box-sizing: border-box;
}

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
  margin: 4px 0 0;

  font-size: 12px;

  color: #94a3b8;
}

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
}

/* HEADER */

.topbar {
  min-height: 72px;

  display: flex;

  align-items: center;

  justify-content: space-between;

  gap: 20px;

  padding: 14px 24px;

  background: white;

  border-bottom:
    1px solid #e2e8f0;
}

.topbar h2 {
  margin: 0;

  font-size: 20px;

  font-weight: 600;
}

.subtitulo {
  margin: 4px 0 0;

  font-size: 12px;

  color: #64748b;
}

/* BOTÃO PRINCIPAL */

.btn-nova {
  padding: 11px 17px;

  border: none;

  border-radius: 8px;

  background: #2563eb;

  color: white;

  font-size: 14px;

  font-weight: 600;

  cursor: pointer;

  transition:
    background 0.15s,
    transform 0.15s;
}

.btn-nova:hover {
  background: #1d4ed8;

  transform:
    translateY(-1px);
}

/* PÁGINA */

.pagina {
  padding: 20px;
}

/* FORMULÁRIO DISCIPLINA */

.formulario-card {
  max-width: 650px;

  margin-bottom: 25px;

  padding: 22px;

  background: white;

  border:
    1px solid #dbeafe;

  border-radius: 12px;

  box-shadow:
    0 3px 12px
    rgba(15, 23, 42, 0.06);
}

.formulario-topo {
  display: flex;

  align-items: flex-start;

  justify-content: space-between;
}

.formulario-topo h3 {
  margin: 0;

  font-size: 18px;
}

.formulario-topo p {
  margin: 4px 0 0;

  font-size: 13px;

  color: #64748b;
}

.formulario {
  margin-top: 20px;
}

/* CAMPOS */

.campo {
  display: flex;

  flex-direction: column;

  gap: 7px;
}

.campo label {
  font-size: 13px;

  font-weight: 600;
}

.campo input {
  width: 100%;

  padding: 12px;

  border:
    1px solid #cbd5e1;

  border-radius: 8px;

  outline: none;

  background: white;

  font-size: 14px;

  transition: 0.15s;
}

.campo input:focus {
  border-color: #2563eb;

  box-shadow:
    0 0 0 3px #dbeafe;
}

.linha-campos {
  display: grid;

  grid-template-columns:
    1fr 1fr;

  gap: 14px;
}

.erro-formulario {
  margin: 10px 0 0;

  font-size: 13px;

  color: #dc2626;
}

.acoes-formulario {
  display: flex;

  justify-content: flex-end;

  gap: 10px;

  margin-top: 20px;
}

/* BOTÕES */

.btn-fechar {
  width: 36px;

  height: 36px;

  flex-shrink: 0;

  border: none;

  border-radius: 50%;

  background: transparent;

  color: #64748b;

  font-size: 25px;

  cursor: pointer;
}

.btn-fechar:hover {
  background: #f1f5f9;

  color: #0f172a;
}

.btn-cancelar {
  padding: 10px 16px;

  border:
    1px solid #cbd5e1;

  border-radius: 8px;

  background: white;

  color: #334155;

  cursor: pointer;
}

.btn-cancelar:hover {
  background: #f8fafc;
}

.btn-salvar {
  padding: 10px 17px;

  border: none;

  border-radius: 8px;

  background: #2563eb;

  color: white;

  font-weight: 600;

  cursor: pointer;
}

.btn-salvar:hover {
  background: #1d4ed8;
}

.btn-salvar:disabled {
  opacity: 0.6;

  cursor: not-allowed;
}

/* MENSAGEM */

.mensagem-sucesso {
  margin-bottom: 20px;

  padding: 12px 15px;

  border:
    1px solid #bbf7d0;

  border-radius: 8px;

  background: #f0fdf4;

  color: #15803d;

  font-size: 13px;
}

/* GRID */

.grid-disciplinas {
  display: grid;

  grid-template-columns:
    repeat(
      auto-fill,
      minmax(
        270px,
        1fr
      )
    );

  gap: 18px;
}

/* CARD */

.disciplina-card {
  overflow: hidden;

  background: white;

  border:
    1px solid #e2e8f0;

  border-radius: 12px;

  transition:
    transform 0.18s,
    box-shadow 0.18s,
    border-color 0.18s;
}

.disciplina-card:hover {
  transform:
    translateY(-3px);

  border-color: #bfdbfe;

  box-shadow:
    0 8px 20px
    rgba(15, 23, 42, 0.08);
}

.disciplina-conteudo {
  width: 100%;

  min-height: 140px;

  display: flex;

  align-items: center;

  gap: 15px;

  padding: 20px;

  border: none;

  background: transparent;

  color: inherit;

  text-align: left;

  cursor: pointer;
}

.disciplina-conteudo:hover {
  background: #f8fbff;
}

.icone-disciplina {
  width: 48px;

  height: 48px;

  flex-shrink: 0;

  display: flex;

  align-items: center;

  justify-content: center;

  border-radius: 12px;

  background: #dbeafe;

  color: #1d4ed8;

  font-size: 20px;

  font-weight: 700;
}

.disciplina-info {
  min-width: 0;
}

.disciplina-info h3 {
  margin: 0;

  font-size: 16px;

  overflow: hidden;

  text-overflow: ellipsis;
}

.disciplina-info p {
  margin: 5px 0 0;

  font-size: 12px;

  color: #64748b;
}

.resumo-horario {
  color: #2563eb !important;

  font-weight: 500;
}

.card-footer {
  display: flex;

  align-items: center;

  justify-content: space-between;

  padding: 12px 18px;

  border-top:
    1px solid #f1f5f9;

  background: #fafafa;
}

.btn-acessar {
  padding: 6px 8px;

  border: none;

  border-radius: 6px;

  background: transparent;

  color: #2563eb;

  font-size: 13px;

  font-weight: 600;

  cursor: pointer;
}

.btn-acessar:hover {
  background: #eff6ff;
}

.btn-excluir {
  padding: 7px 9px;

  border: none;

  border-radius: 6px;

  background: transparent;

  color: #dc2626;

  font-size: 12px;

  cursor: pointer;
}

.btn-excluir:hover {
  background: #fef2f2;
}

/* VAZIO */

.vazio {
  min-height: 430px;

  display: flex;

  flex-direction: column;

  align-items: center;

  justify-content: center;

  text-align: center;
}

.icone-vazio {
  width: 65px;

  height: 65px;

  display: flex;

  align-items: center;

  justify-content: center;

  margin-bottom: 15px;

  border-radius: 18px;

  background: #dbeafe;

  color: #2563eb;

  font-size: 28px;
}

.vazio h3 {
  margin: 0;

  font-size: 18px;
}

.vazio p {
  max-width: 380px;

  margin: 8px 0 20px;

  color: #64748b;

  font-size: 13px;
}

/* ESTADO */

.estado {
  padding: 50px;

  text-align: center;

  color: #64748b;
}

.estado.erro {
  color: #dc2626;
}

/* FUNDO MODAL */

.modal-fundo {
  position: fixed;

  inset: 0;

  z-index: 100;

  display: flex;

  align-items: center;

  justify-content: center;

  padding: 24px;

  background:
    rgba(
      15,
      23,
      42,
      0.48
    );

  backdrop-filter:
    blur(2px);
}

/* MODAL DISCIPLINA */

.modal-disciplina {
  width: 100%;

  max-width: 760px;

  max-height: 88vh;

  display: flex;

  flex-direction: column;

  overflow: hidden;

  background: white;

  border:
    1px solid #e2e8f0;

  border-radius: 14px;

  box-shadow:
    0 22px 60px
    rgba(
      15,
      23,
      42,
      0.25
    );
}

.modal-disciplina-topo {
  display: flex;

  align-items: center;

  justify-content: space-between;

  gap: 20px;

  padding: 20px 22px;

  border-bottom:
    1px solid #e2e8f0;
}

.titulo-disciplina-modal {
  display: flex;

  align-items: center;

  gap: 14px;
}

.titulo-disciplina-modal h2 {
  margin: 0;

  font-size: 20px;

  font-weight: 600;
}

.titulo-disciplina-modal p {
  margin: 4px 0 0;

  color: #64748b;

  font-size: 12px;
}

.modal-disciplina-corpo {
  padding: 22px;

  overflow-y: auto;
}

/* HORÁRIO RECORRENTE */

.horario-recorrente {
  width: 100%;
}

.horario-topo {
  display: flex;

  align-items: center;

  justify-content: space-between;

  gap: 20px;
}

.horario-topo h3 {
  margin: 0;

  font-size: 16px;
}

.horario-topo p {
  margin: 4px 0 0;

  color: #64748b;

  font-size: 12px;
}

.btn-nova-aula {
  padding: 9px 14px;

  flex-shrink: 0;

  border: none;

  border-radius: 7px;

  background: #2563eb;

  color: white;

  font-size: 12px;

  font-weight: 600;

  cursor: pointer;
}

.btn-nova-aula:hover {
  background: #1d4ed8;
}

.btn-editar-horario {
  padding: 8px 13px;

  flex-shrink: 0;

  border:
    1px solid #bfdbfe;

  border-radius: 7px;

  background: white;

  color: #2563eb;

  font-size: 12px;

  font-weight: 600;

  cursor: pointer;
}

.btn-editar-horario:hover {
  background: #eff6ff;
}

/* FORM HORÁRIO */

.form-horario {
  margin-top: 20px;

  padding: 20px;

  border:
    1px solid #dbeafe;

  border-radius: 10px;

  background: #f8fbff;

  display: flex;

  flex-direction: column;

  gap: 18px;
}

/* DIAS DA SEMANA */

.dias-semana {
  display: flex;

  flex-wrap: wrap;

  gap: 8px;

  margin-top: 3px;
}

.dia-semana {
  min-width: 48px;

  padding: 9px 12px;

  border:
    1px solid #cbd5e1;

  border-radius: 8px;

  background: white;

  color: #475569;

  font-size: 12px;

  font-weight: 600;

  cursor: pointer;

  transition:
    background 0.15s,
    border-color 0.15s,
    color 0.15s,
    transform 0.15s;
}

.dia-semana:hover {
  border-color: #93c5fd;

  background: #eff6ff;

  color: #1d4ed8;

  transform:
    translateY(-1px);
}

.dia-semana.selecionado {
  border-color: #2563eb;

  background: #2563eb;

  color: white;
}

/* HORÁRIO SALVO */

.horario-salvo {
  margin-top: 20px;

  padding: 18px;

  border:
    1px solid #dbeafe;

  border-radius: 10px;

  background: #f8fbff;
}

.horario-principal {
  display: flex;

  align-items: center;

  justify-content: space-between;

  gap: 15px;
}

.horario-principal strong {
  color: #1e3a8a;

  font-size: 15px;
}

.dias-salvos {
  display: flex;

  flex-wrap: wrap;

  gap: 6px;
}

.dia-chip-salvo {
  padding: 5px 9px;

  border-radius: 20px;

  background: #dbeafe;

  color: #1d4ed8;

  font-size: 11px;

  font-weight: 600;
}

.periodo-horario {
  display: flex;

  align-items: center;

  gap: 8px;

  margin-top: 13px;

  color: #64748b;

  font-size: 12px;
}

.horario-observacao {
  margin-top: 14px;

  padding-top: 12px;

  border-top:
    1px solid #dbeafe;

  color: #94a3b8;

  font-size: 11px;
}

/* SEM HORÁRIO */

.sem-horario {
  margin-top: 24px;

  padding: 38px 20px;

  border:
    1px dashed #cbd5e1;

  border-radius: 10px;

  text-align: center;

  background: #f8fafc;
}

.sem-horario-icone {
  width: 46px;

  height: 46px;

  display: flex;

  align-items: center;

  justify-content: center;

  margin:
    0 auto 12px;

  border-radius: 50%;

  background: #dbeafe;

  color: #2563eb;

  font-size: 22px;
}

.sem-horario h4 {
  margin: 0;

  font-size: 14px;
}

.sem-horario p {
  max-width: 380px;

  margin:
    6px auto 0;

  color: #64748b;

  font-size: 12px;
}

/* MODAL EXCLUSÃO */

.modal-exclusao {
  width: 100%;

  max-width: 400px;

  padding: 25px;

  border-radius: 14px;

  background: white;

  text-align: center;

  box-shadow:
    0 20px 50px
    rgba(
      15,
      23,
      42,
      0.2
    );
}

.modal-icone {
  width: 50px;

  height: 50px;

  margin:
    0 auto 14px;

  display: flex;

  align-items: center;

  justify-content: center;

  border-radius: 50%;

  background: #fee2e2;

  color: #dc2626;

  font-size: 23px;

  font-weight: 700;
}

.modal-exclusao h3 {
  margin: 0;

  font-size: 19px;
}

.modal-exclusao p {
  margin: 10px 0 0;

  color: #475569;

  font-size: 13px;
}

.modal-exclusao .aviso {
  color: #dc2626;
}

.modal-acoes {
  display: flex;

  justify-content: flex-end;

  gap: 10px;

  margin-top: 24px;
}

.btn-confirmar-exclusao {
  padding: 10px 16px;

  border: none;

  border-radius: 8px;

  background: #dc2626;

  color: white;

  font-weight: 600;

  cursor: pointer;
}

.btn-confirmar-exclusao:hover {
  background: #b91c1c;
}

.btn-confirmar-exclusao:disabled {
  opacity: 0.6;

  cursor: not-allowed;
}

/* RESPONSIVO */

@media (max-width: 800px) {
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

  .grid-disciplinas {
    grid-template-columns:
      1fr;
  }

  .linha-campos {
    grid-template-columns:
      1fr;
  }

  .horario-principal {
    align-items:
      flex-start;

    flex-direction:
      column;
  }

  .horario-topo {
    align-items:
      flex-start;

    flex-direction:
      column;
  }

  .modal-disciplina {
    max-height: 92vh;
  }
}
</style>