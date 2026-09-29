<template>
  <div
    class="min-h-screen bg-[linear-gradient(180deg,#dbeafe_0%,#eff6ff_45%,#ffffff_100%)] flex items-center justify-center px-4"
  >
    <div class="w-full max-w-md rounded-2xl bg-white shadow-xl p-8 border border-blue-100">
      <div class="text-center mb-6">
        <h1 class="text-2xl font-semibold text-slate-900">
          Redefinir senha
        </h1>

        <p class="text-sm text-slate-500 mt-2">
          Informe seus dados para criar uma nova senha.
        </p>
      </div>

      <form class="space-y-4" @submit.prevent="salvarNovaSenha">
        <input
          v-model="email"
          type="email"
          placeholder="E-mail"
          class="w-full rounded-lg border border-slate-300 px-3 py-3 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
        />

        <div class="relative">
          <input
            v-model="novaSenha"
            :type="mostrarSenha1 ? 'text' : 'password'"
            placeholder="Nova senha"
            class="w-full rounded-lg border border-slate-300 px-3 py-3 pr-20 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
          />

          <button
            type="button"
            class="absolute right-3 top-1/2 -translate-y-1/2 text-sm text-blue-600 hover:text-blue-700"
            @click="mostrarSenha1 = !mostrarSenha1"
          >
            {{ mostrarSenha1 ? 'Ocultar' : 'Ver' }}
          </button>
        </div>

        <div class="relative">
          <input
            v-model="confirmarSenha"
            :type="mostrarSenha2 ? 'text' : 'password'"
            placeholder="Confirmar nova senha"
            class="w-full rounded-lg border border-slate-300 px-3 py-3 pr-20 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
          />

          <button
            type="button"
            class="absolute right-3 top-1/2 -translate-y-1/2 text-sm text-blue-600 hover:text-blue-700"
            @click="mostrarSenha2 = !mostrarSenha2"
          >
            {{ mostrarSenha2 ? 'Ocultar' : 'Ver' }}
          </button>
        </div>

        <p v-if="erro" class="text-sm text-red-500">
          {{ erro }}
        </p>

        <p v-if="sucesso" class="text-sm text-green-600">
          {{ sucesso }}
        </p>

        <button
          type="submit"
          class="w-full rounded-lg bg-blue-600 py-3 text-sm font-medium text-white transition hover:bg-blue-700"
        >
          Salvar nova senha
        </button>

        <div class="text-center">
          <NuxtLink
            to="/"
            class="text-sm text-blue-600 hover:text-blue-700 hover:underline"
          >
            Voltar para login
          </NuxtLink>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
const email = ref('')
const novaSenha = ref('')
const confirmarSenha = ref('')

const mostrarSenha1 = ref(false)
const mostrarSenha2 = ref(false)

const erro = ref('')
const sucesso = ref('')

function salvarNovaSenha() {
  erro.value = ''
  sucesso.value = ''

  if (
    !email.value.trim() ||
    !novaSenha.value.trim() ||
    !confirmarSenha.value.trim()
  ) {
    erro.value = 'Preencha todos os campos.'
    return
  }

  if (novaSenha.value !== confirmarSenha.value) {
    erro.value = 'As senhas não coincidem.'
    return
  }

  sucesso.value = 'Senha alterada com sucesso.'
}
</script>