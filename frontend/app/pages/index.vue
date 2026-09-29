<template>
  <div
    class="min-h-screen bg-[linear-gradient(180deg,#dbeafe_0%,#eff6ff_45%,#ffffff_100%)] flex items-center justify-center px-4"
  >
    <div
      class="w-full max-w-md rounded-2xl bg-white shadow-xl p-8 border border-blue-100"
    >
      <div class="text-center mb-6">
        <h1 class="text-2xl font-semibold text-slate-900">
          Bem-vindo de volta!
        </h1>

        <p class="text-sm text-slate-500 mt-2">
          Não tem uma conta?

          <NuxtLink
            to="/cadastro"
            class="text-blue-600 hover:text-blue-700 hover:underline"
          >
            Criar uma conta
          </NuxtLink>
        </p>
      </div>

      <form class="space-y-4" @submit.prevent="entrar">
        <input
          v-model="email"
          type="email"
          placeholder="E-mail"
          class="w-full rounded-lg border px-3 py-3 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
          :class="erro ? 'border-red-400' : 'border-slate-300'"
        />

        <div class="relative">
          <input
            v-model="senha"
            :type="mostrarSenha ? 'text' : 'password'"
            placeholder="Senha"
            class="w-full rounded-lg border border-slate-300 px-3 py-3 pr-12 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
          />

          <button
            type="button"
            class="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 transition hover:text-blue-600"
            :aria-label="mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'"
            @click="mostrarSenha = !mostrarSenha"
          >
            <svg
              v-if="!mostrarSenha"
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              class="h-5 w-5"
            >
              <path
                d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12"
              ></path>

              <circle
                cx="12"
                cy="12"
                r="3"
              ></circle>
            </svg>

            <svg
              v-else
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              class="h-5 w-5"
            >
              <path d="M3 3l18 18"></path>

              <path
                d="M10.6 10.6a2 2 0 0 0 2.8 2.8"
              ></path>

              <path
                d="M9.9 5.1A10.8 10.8 0 0 1 12 5c6.5 0 10 7 10 7a18.4 18.4 0 0 1-3 4"
              ></path>

              <path
                d="M6.6 6.6C3.8 8.4 2 12 2 12s3.5 7 10 7a10.6 10.6 0 0 0 4.1-.8"
              ></path>
            </svg>
          </button>
        </div>

        <p
          v-if="erro"
          class="text-sm text-red-500"
        >
          {{ erro }}
        </p>

        <button
          type="submit"
          class="w-full rounded-lg bg-blue-600 py-3 text-sm font-medium text-white transition hover:bg-blue-700"
        >
          Entrar
        </button>

        <div class="text-center">
          <NuxtLink
            to="/esqueci-senha"
            class="text-sm text-blue-600 hover:text-blue-700 hover:underline"
          >
            Esqueceu a senha?
          </NuxtLink>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
const email = ref('')
const senha = ref('')
const mostrarSenha = ref(false)
const erro = ref('')

async function entrar() {
  erro.value = ''

  if (!email.value.trim() || !senha.value.trim()) {
    erro.value = 'Preencha e-mail e senha.'
    return
  }

  if (
    email.value === 'admin@gmail.com' &&
    senha.value === '123'
  ) {
    await navigateTo('/agenda')
    return
  }

  erro.value = 'E-mail ou senha inválidos.'
}
</script>