<template>
  <div class="min-h-screen w-full bg-slate-100 p-6">
    <div class="max-w-2xl mx-auto space-y-6">

      <button
        class="text-sm text-slate-500 hover:underline"
        @click="router.back()"
      >
        &larr; Voltar
      </button>

      <div>
        <h1 class="text-xl font-semibold text-slate-800">
          {{ aula?.disciplinaNome ?? (erro ? 'Aula' : 'Carregando...') }}
        </h1>

        <p v-if="aula" class="text-sm text-slate-500 mt-1">
          {{ aula.data }} · {{ aula.horario }}
          <span v-if="aula.topico">
            — {{ aula.topico }}
          </span>
        </p>
      </div>

      <p
        v-if="erro"
        class="text-sm text-red-600"
        role="alert"
      >
        {{ erro }}
      </p>

      <!-- a mesma área de anotações do modal da aula (listar, criar, editar, excluir e anexar imagens) -->
      <section
        v-if="aula"
        class="bg-white rounded-lg shadow p-4"
      >
        <AnotacoesAula :aula="aula" />
      </section>

    </div>
  </div>
</template>

<script setup lang="ts">
interface Aula {
  id: string
  data: string
  horario: string
  topico: string
  disciplinaNome: string
}

const route = useRoute()
const router = useRouter()

const aulaId = route.params.id as string

const { $api } = useNuxtApp()

const aula = ref<Aula | null>(null)
const erro = ref('')

async function carregarAula() {
  try {
    const { data } =
      await $api.get<Aula>(`/api/aulas/${aulaId}`)

    aula.value = data
  } catch (err) {
    console.error('[aula] erro ao carregar:', err)
    erro.value = mensagemDeErro(err, 'Não foi possível carregar a aula.')
  }
}

onMounted(() => {
  carregarAula()
})
</script>
