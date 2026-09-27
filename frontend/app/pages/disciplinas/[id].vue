<template>
  <div class="min-h-screen bg-slate-100 p-6">
    <div class="max-w-md mx-auto space-y-6">
      <NuxtLink to="/disciplinas" class="text-sm text-slate-500 hover:underline">
        &larr; Disciplinas
      </NuxtLink>

      <h1 class="text-xl font-semibold text-slate-800">
        {{ disciplina?.nome ?? 'Carregando...' }}
      </h1>

      <form @submit.prevent="cadastrar" class="bg-white rounded-lg shadow p-4 space-y-3">
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">Data</label>
          <input
            v-model="data"
            type="date"
            class="w-full rounded-md border px-3 py-2 text-sm"
            :class="erro ? 'border-red-500' : 'border-slate-300'"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">Horário</label>
          <input
            v-model="horario"
            type="text"
            placeholder="ex: 08:50 - 10:30"
            class="w-full rounded-md border px-3 py-2 text-sm"
            :class="erro ? 'border-red-500' : 'border-slate-300'"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">Tópico</label>
          <input
            v-model="topico"
            type="text"
            class="w-full rounded-md border px-3 py-2 text-sm border-slate-300"
          />
        </div>
        <p v-if="erro" class="text-xs text-red-600">{{ erro }}</p>
        <button
          type="submit"
          :disabled="salvando"
          class="w-full rounded-md bg-slate-800 text-white py-2 text-sm font-medium hover:bg-slate-700 disabled:opacity-50"
        >
          {{ salvando ? 'Salvando...' : 'Salvar' }}
        </button>
      </form>

      <div class="bg-white rounded-lg shadow p-4">
        <h2 class="text-sm font-medium text-slate-700 mb-2">Aulas cadastradas</h2>
        <p v-if="carregando" class="text-sm text-slate-500">Carregando...</p>
        <p v-else-if="aulasOrdenadas.length === 0" class="text-sm text-slate-500">
          Nenhuma aula cadastrada ainda.
        </p>
        <ul v-else class="divide-y divide-slate-100">
          <li v-for="a in aulasOrdenadas" :key="a.id" class="py-2 text-sm text-slate-800">
            <span class="font-medium">{{ a.data }}</span>
            <span class="text-slate-500"> · {{ a.horario }}</span>
            <span v-if="a.topico"> — {{ a.topico }}</span>
          </li>
        </ul>
      </div>
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