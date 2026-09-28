<template>
  <div class="min-h-screen bg-slate-100 p-6">
    <div class="max-w-md mx-auto space-y-6">
      <h1 class="text-xl font-semibold text-slate-800">Disciplinas</h1>

      <form @submit.prevent="cadastrar" class="bg-white rounded-lg shadow p-4 space-y-3">
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">Nome</label>
          <input
            v-model="nome"
            type="text"
            class="w-full rounded-md border px-3 py-2 text-sm"
            :class="erro ? 'border-red-500' : 'border-slate-300'"
          />
          <p v-if="erro" class="text-xs text-red-600 mt-1">{{ erro }}</p>
        </div>
        <button
          type="submit"
          :disabled="salvando"
          class="w-full rounded-md bg-slate-800 text-white py-2 text-sm font-medium hover:bg-slate-700 disabled:opacity-50"
        >
          {{ salvando ? 'Salvando...' : 'Salvar' }}
        </button>
      </form>

      <div class="bg-white rounded-lg shadow p-4">
        <h2 class="text-sm font-medium text-slate-700 mb-2">Disciplinas cadastradas</h2>
        <p v-if="carregando" class="text-sm text-slate-500">Carregando...</p>
        <p v-else-if="disciplinas.length === 0" class="text-sm text-slate-500">
          Nenhuma disciplina cadastrada ainda.
        </p>
        <ul v-else class="divide-y divide-slate-100">
            <li v-for="d in disciplinas" :key="d.id" class="py-2">
                <NuxtLink :to="`/disciplinas/${d.id}`" class="text-sm text-slate-800 hover:underline">
                {{ d.nome }}
                </NuxtLink>
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

const { $api } = useNuxtApp()

const nome = ref('')
const erro = ref('')
const salvando = ref(false)
const carregando = ref(true)
const disciplinas = ref<Disciplina[]>([])

async function listar() {
  carregando.value = true
  try {
    const { data } = await $api.get<Disciplina[]>('/api/disciplinas')
    disciplinas.value = data
  } catch (err) {
    console.error('[disciplinas] falha ao listar:', err)
  } finally {
    carregando.value = false
  }
}

async function cadastrar() {
  if (!nome.value.trim()) {
    erro.value = 'O nome é obrigatório.'
    return
  }
  erro.value = ''
  salvando.value = true
  try {
    const { data } = await $api.post<Disciplina>('/api/disciplinas', { nome: nome.value.trim() })
    disciplinas.value.push(data)
    nome.value = ''
  } catch (err) {
    console.error('[disciplinas] falha ao cadastrar:', err)
    erro.value = 'Não foi possível salvar. Tente novamente.'
  } finally {
    salvando.value = false
  }
}

onMounted(listar)
</script>