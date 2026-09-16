<template>
  <div class="min-h-screen flex items-center justify-center bg-slate-100 p-6">
    <div class="max-w-md w-full bg-white rounded-lg shadow p-6 space-y-4">
      <h1 class="text-xl font-semibold text-slate-800">AllGenda — Front-end</h1>
      <p class="text-sm text-slate-500">
        Teste de conexão com o back-end em
        <code class="bg-slate-100 px-1 rounded">{{ apiBase }}</code>
      </p>

      <div
        class="rounded-md p-3 text-sm font-medium"
        :class="{
          'bg-slate-100 text-slate-600': status === 'loading',
          'bg-green-100 text-green-700': status === 'success',
          'bg-red-100 text-red-700': status === 'error'
        }"
      >
        <span v-if="status === 'loading'">Chamando o back-end...</span>
        <span v-else-if="status === 'success'">
          ✅ Back-end respondeu (status {{ resultInfo }}). CORS ok.
        </span>
        <span v-else>
          ❌ Falha ao chamar o back-end: {{ resultInfo }}
        </span>
      </div>

      <button
        class="w-full rounded-md bg-slate-800 text-white py-2 text-sm font-medium hover:bg-slate-700"
        @click="pingBackend"
      >
        Testar novamente
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
const config = useRuntimeConfig()
const apiBase = config.public.apiBase

const status = ref<'loading' | 'success' | 'error'>('loading')
const resultInfo = ref('')

const { $api } = useNuxtApp()

async function pingBackend() {
  status.value = 'loading'
  try {
    // Chamada de teste: qualquer resposta HTTP (mesmo 404) confirma que o
    // navegador conseguiu falar com o back-end e que o CORS está liberado.
    // Ajuste a rota abaixo para um endpoint real assim que ele existir.
    const response = await $api.get('/')
    status.value = 'success'
    resultInfo.value = String(response.status)
    console.log('[allgenda] back-end respondeu com sucesso:', response)
  } catch (err: any) {
    status.value = 'error'
    if (err.response) {
      // O back respondeu (ex: 404), então a conexão e o CORS estão ok.
      status.value = 'success'
      resultInfo.value = String(err.response.status)
      console.log('[allgenda] back-end acessível (respondeu com erro HTTP):', err.response)
    } else {
      resultInfo.value = err.message ?? 'erro desconhecido'
      console.error('[allgenda] falha ao contatar o back-end (rede ou CORS):', err)
    }
  }
}

onMounted(pingBackend)
</script>