<template>
  <div
    class="min-h-screen bg-[linear-gradient(180deg,#dbeafe_0%,#eff6ff_45%,#ffffff_100%)] flex items-center justify-center px-4"
  >
    <div class="w-full max-w-md rounded-2xl bg-white shadow-xl p-8 border border-blue-100">
      <div class="text-center mb-6">
        <h1 class="text-2xl font-semibold text-slate-900">
          Crie sua conta
        </h1>

        <p class="text-sm text-slate-500 mt-2">
          Já tem uma conta?
          <NuxtLink
            to="/"
            class="text-blue-600 hover:text-blue-700 hover:underline"
          >
            Entrar
          </NuxtLink>
        </p>
      </div>

      <form class="space-y-4" @submit.prevent="cadastrar">
        <input
          v-model="nome"
          type="text"
          placeholder="Nome completo"
          class="w-full rounded-lg border border-slate-300 px-3 py-3 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
        />

        <input
          v-model="email"
          type="email"
          placeholder="E-mail"
          class="w-full rounded-lg border border-slate-300 px-3 py-3 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
        />

        <div class="relative">
          <input
            v-model="senha"
            :type="mostrarSenha ? 'text' : 'password'"
            placeholder="Senha"
            class="w-full rounded-lg border border-slate-300 px-3 py-3 pr-20 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
          />

          <button
            type="button"
            class="absolute right-3 top-1/2 -translate-y-1/2 text-sm text-blue-600 hover:text-blue-700"
            @click="mostrarSenha = !mostrarSenha"
          >
            {{ mostrarSenha ? 'Ocultar' : 'Ver' }}
          </button>
        </div>

        <p v-if="erro" class="text-sm text-red-500">
          {{ erro }}
        </p>

        <button
          type="submit"
          class="w-full rounded-lg bg-blue-600 py-3 text-sm font-medium text-white transition hover:bg-blue-700"
        >
          Cadastrar-se
        </button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">

definePageMeta({
    layout:'auth'
})

</script>
const nome = ref('')
const email = ref('')
const senha = ref('')
const mostrarSenha = ref(false)
const erro = ref('')

async function cadastrar() {
  erro.value = ''

  if (!nome.value.trim() || !email.value.trim() || !senha.value.trim()) {
    erro.value = 'Preencha todos os campos.'
    return
  }

  await navigateTo('/disciplinas')
}
</script>