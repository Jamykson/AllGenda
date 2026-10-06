<template>
  <div class="pagina-disciplina">
    <div class="conteudo-pagina">
      <NuxtLink to="/disciplinas" class="voltar-disciplinas">
        &larr; Disciplinas
      </NuxtLink>

      <h1 class="titulo-disciplina">
        {{ disciplina?.nome ?? 'Carregando...' }}
      </h1>

      <form @submit.prevent="cadastrar" class="formulario-aula">
        <div class="campo-formulario">
          <label class="campo-label">Data</label>
          <input
            v-model="data"
            type="date"
            class="campo-input"
            :class="erro ? 'com-erro' : ''"
          />
        </div>
        <div class="campo-formulario">
          <label class="campo-label">Horário</label>
          <input
            v-model="horario"
            type="text"
            placeholder="ex: 08:50 - 10:30"
            class="campo-input"
            :class="erro ? 'com-erro' : ''"
          />
        </div>
        <div class="campo-formulario">
          <label class="campo-label">Tópico</label>
          <input
            v-model="topico"
            type="text"
            class="campo-input"
          />
        </div>
        <p v-if="erro" class="erro-formulario">{{ erro }}</p>
        <button
          type="submit"
          :disabled="salvando"
          class="botao-salvar"
        >
          {{ salvando ? 'Salvando...' : 'Salvar' }}
        </button>
      </form>

      <section class="lista-aulas">
        <h2 class="titulo-aulas">Aulas cadastradas</h2>
        <p v-if="carregando" class="estado-lista">Carregando...</p>
        <p v-else-if="aulasOrdenadas.length === 0" class="estado-lista">
          Nenhuma aula cadastrada ainda.
        </p>
        <ul v-else class="itens-aulas">
          <li v-for="a in aulasOrdenadas" :key="a.id" class="item-aula">
            <NuxtLink
              :to="`/aulas/${a.id}`"
              class="link-aula"
            >
              <span class="data-aula">{{ a.data }}</span>
              <span class="detalhe-aula"> · {{ a.horario }}</span>
              <span v-if="a.topico"> — {{ a.topico }}</span>
            </NuxtLink>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Disciplina {
  id: string
  nome: string
}

interface Aula {
  id: string
  data: string
  horario: string
  topico: string
  disciplinaNome: string
}

const route = useRoute()
const disciplinaId = route.params.id as string

const { $api } = useNuxtApp()

const disciplina = ref<Disciplina | null>(null)
const data = ref('')
const horario = ref('')
const topico = ref('')
const erro = ref('')
const salvando = ref(false)
const carregando = ref(true)
const aulas = ref<Aula[]>([])

const aulasOrdenadas = computed(() =>
  [...aulas.value].sort((a, b) => {
    if (a.data !== b.data) return a.data.localeCompare(b.data)
    return a.horario.localeCompare(b.horario)
  })
)

async function carregarDisciplina() {
  const { data: resp } = await $api.get<Disciplina>(`/api/disciplinas/${disciplinaId}`)
  disciplina.value = resp
}

async function listarAulas() {
  carregando.value = true
  try {
    const { data: resp } = await $api.get<Aula[]>('/api/aulas', {
      params: { disciplinaId }
    })
    aulas.value = resp
  } catch (err) {
    console.error('[aulas] falha ao listar:', err)
  } finally {
    carregando.value = false
  }
}

async function cadastrar() {
  if (!data.value || !horario.value.trim()) {
    erro.value = 'Data e horário são obrigatórios.'
    return
  }
  erro.value = ''
  salvando.value = true
  try {
    const { data: nova } = await $api.post<Aula>('/api/aulas', {
      disciplinaId,
      data: data.value,
      horario: horario.value.trim(),
      topico: topico.value.trim()
    })
    aulas.value.push(nova)
    data.value = ''
    horario.value = ''
    topico.value = ''
  } catch (err) {
    console.error('[aulas] falha ao cadastrar:', err)
    erro.value = 'Não foi possível salvar. Tente novamente.'
  } finally {
    salvando.value = false
  }
}

onMounted(() => {
  carregarDisciplina()
  listarAulas()
})
</script>

<style scoped>
.pagina-disciplina {
  width: 100%;
}

.conteudo-pagina {
  display: grid;
  width: min(100%, 760px);
  gap: 12px;
  margin: 0 auto;
}

.voltar-disciplinas {
  width: fit-content;
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
  text-decoration: none;
}

.voltar-disciplinas:hover {
  color: #1d4ed8;
  text-decoration: underline;
}

.titulo-disciplina {
  color: #1f2937;
  font-size: 20px;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.formulario-aula,
.lista-aulas {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.campo-formulario {
  display: grid;
  gap: 5px;
}

.campo-label,
.titulo-aulas {
  color: #334155;
  font-size: 12px;
  font-weight: 650;
}

.campo-input {
  width: 100%;
  min-height: 36px;
  padding: 7px 9px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #fff;
  color: #1f2937;
  font: inherit;
  font-size: 13px;
}

.campo-input:focus {
  border-color: #2563eb;
  outline: 2px solid rgb(37 99 235 / 12%);
}

.campo-input.com-erro {
  border-color: #dc2626;
}

.erro-formulario {
  color: #b91c1c;
  font-size: 12px;
}

.botao-salvar {
  min-height: 36px;
  justify-self: start;
  padding: 0 14px;
  border: 1px solid #2563eb;
  border-radius: 6px;
  background: #2563eb;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.botao-salvar:hover:not(:disabled) {
  background: #1d4ed8;
}

.botao-salvar:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.itens-aulas {
  padding: 0;
  list-style: none;
}

.item-aula + .item-aula {
  border-top: 1px solid #eef2f7;
}

.link-aula {
  display: block;
  padding: 9px 8px;
  border-radius: 6px;
  color: #334155;
  font-size: 13px;
  text-decoration: none;
}

.link-aula:hover {
  background: #f8fafc;
}

.data-aula {
  color: #1f2937;
  font-weight: 600;
}

.detalhe-aula,
.estado-lista {
  color: #64748b;
}

.estado-lista {
  font-size: 13px;
}

.link-aula:focus-visible,
.voltar-disciplinas:focus-visible,
.botao-salvar:focus-visible {
  outline: 2px solid #2563eb;
  outline-offset: 2px;
}
</style>