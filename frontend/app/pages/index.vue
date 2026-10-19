<template>
  <div class="auth-page">

    <main class="auth-shell">
      <section class="auth-content">
        <div class="brand">
          <h1 class="brand-name">AllGenda</h1>
        </div>

        <div class="headline">
          <h2>Bem-vindo de volta!</h2>

          <p>
            Não tem uma conta?
            <NuxtLink to="/cadastro">
              Criar uma conta
            </NuxtLink>
          </p>
        </div>

        <form
          class="form"
          @submit.prevent="entrar"
        >
          <p v-if="aviso" role="status" class="mb-3 text-sm text-blue-700">{{ aviso }}</p>
          <div class="field-group">
            <input
              v-model="email"
              type="email"
              aria-label="E-mail"
              autocomplete="email"
              placeholder="E-mail"
              class="field"
              :class="{ 'field-error': erroEmail }"
              @input="erroEmail = ''"
            />

            <p
              v-if="erroEmail"
              class="field-message"
            >
              {{ erroEmail }}
            </p>
          </div>

          <div class="password-wrapper">
            <input
              v-model="senha"
              :type="mostrarSenha ? 'text' : 'password'"
              aria-label="Senha"
              autocomplete="current-password"
              placeholder="Senha"
              class="field field-password"
            />

            <button
              type="button"
              class="password-toggle"
              :aria-label="mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'"
              @click="mostrarSenha = !mostrarSenha"
            >
              <svg
                v-if="!mostrarSenha"
                xmlns="http://www.w3.org/2000/svg"
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12" />
                <circle cx="12" cy="12" r="3" />
              </svg>

              <svg
                v-else
                xmlns="http://www.w3.org/2000/svg"
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M3 3l18 18" />
                <path d="M10.6 10.6a2 2 0 0 0 2.8 2.8" />
                <path d="M9.9 5.1A10.8 10.8 0 0 1 12 5c6.5 0 10 7 10 7a18.4 18.4 0 0 1-3 4" />
                <path d="M6.6 6.6C3.8 8.4 2 12 2 12s3.5 7 10 7a10.6 10.6 0 0 0 4.1-.8" />
              </svg>
            </button>
          </div>

          <p
            v-if="erroLogin"
            class="login-message"
            role="alert"
          >
            {{ erroLogin }}
          </p>

          <button
            type="submit"
            class="submit-button"
            :disabled="!formValido || enviando"
          >
            {{ enviando ? 'Entrando…' : 'Entrar' }}
          </button>

          <NuxtLink
            to="/esqueci-senha"
            class="forgot-link"
          >
            Esqueceu a senha?
          </NuxtLink>
        </form>
      </section>
    </main>

    <footer class="auth-footer">
      Precisa de ajuda?
    </footer>
  </div>
</template>

<script setup lang="ts">
import { mensagemDeErro } from '~/composables/useAnotacoes'

definePageMeta({ layout: 'auth' })
const auth = useAuth()
const route = useRoute()
const email = ref('')
const senha = ref('')
const mostrarSenha = ref(false)
const erroEmail = ref('')
const erroLogin = ref('')
const enviando = ref(false)
const formValido = computed(() => email.value.trim() !== '' && senha.value !== '')
const aviso = computed(() => route.query.cadastro === 'sucesso'
  ? 'Conta criada! Entre com seu e-mail e senha.'
  : route.query.sessao === 'expirada' ? 'Sua sessão expirou. Entre novamente.' : '')

async function entrar() {
  if (enviando.value) return
  erroEmail.value = ''
  erroLogin.value = ''
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim())) {
    erroEmail.value = 'Informe um e-mail válido.'
    return
  }
  if (!senha.value) {
    erroLogin.value = 'Informe sua senha.'
    return
  }
  enviando.value = true
  try {
    await auth.entrar(email.value.trim(), senha.value)
    // Permite retornar somente para caminhos internos da aplicação.
    const retorno = route.query.retorno
    await navigateTo(typeof retorno === 'string' && /^\/(?![\/\\])/.test(retorno) ? retorno : '/agenda')
  } catch (erro) {
    erroLogin.value = mensagemDeErro(erro, 'Não foi possível entrar. Confira os dados e tente novamente.')
  } finally {
    enviando.value = false
  }
}
</script>

<style scoped>

.auth-page {

  position:relative;

  width:100%;

  min-height:100vh;

  background:
    radial-gradient(
      circle at 15% 0%,
      rgba(147,197,253,.45),
      transparent 45%
    ),

    radial-gradient(
      circle at 85% 0%,
      rgba(96,165,250,.35),
      transparent 45%
    ),

    linear-gradient(
      180deg,
      #eff6ff 0%,
      #ffffff 45%
    );

  color:#0f172a;

}



.auth-shell {

  min-height:calc(100vh - 48px);

  display:flex;

  justify-content:center;

  align-items:flex-start;

  padding:68px 24px 20px;

}



.auth-content {

  width:100%;

  max-width:400px;

}



.brand {

  text-align:center;

  margin-bottom:14px;

}



.brand-name {

  margin-top:6px;

  font-size:22px;

  font-weight:800;

}



.headline {

  text-align:center;

  margin-bottom:18px;

}



.headline h2 {

  margin:0;

  font-size:18px;

  font-weight:800;

}



.headline p {

  margin-top:5px;

  font-size:12px;

  color:#64748b;

}



.headline a,
.forgot-link {

  color:#2563eb;

  text-decoration:none;

}



.social-card,
.sso-button,
.field,
.submit-button {

  width:100%;

  border-radius:8px;

}

.divider span {

  height:1px;

  background:#dbe2ea;

}



.divider small {

  color:#94a3b8;

}



.form {

  display:flex;

  flex-direction:column;

}



.field-group {

  margin-bottom:10px;

}



.field {

  height:38px;

  border:1px solid #cbd5e1;

  padding:0 12px;

  font-size:12px;

  outline:none;

}



.field:focus {

  border-color:#2563eb;

}



.password-wrapper {

  position:relative;

}



.field-password {

  padding-right:35px;

}



.password-toggle {

  position:absolute;

  right:10px;

  top:50%;

  transform:translateY(-50%);

  border:none;

  background:none;

  color:#64748b;

}



.login-message,
.field-message {

  font-size:11px;

  color:#dc2626;

}



.submit-button {

  height:38px;

  margin-top:0;

  background:#2563eb;

  border:none;

  color:white;

  font-size:12px;

  font-weight:700;

}



.submit-button:disabled {

  background:#cbd5e1;

}



.forgot-link {

  display:block;

  text-align:center;

  margin-top:12px;

  font-size:12px;

}



.auth-footer {

  text-align:center;

  padding:20px;

  color:#64748b;

  font-size:11px;

}



</style>