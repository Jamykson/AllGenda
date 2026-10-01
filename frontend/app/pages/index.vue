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
          <div class="field-group">
            <input
              v-model="email"
              type="email"
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
              placeholder="Senha"
              class="field field-password"
            />

            <button
              type="button"
              class="password-toggle"
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
          >
            {{ erroLogin }}
          </p>

          <button
            type="submit"
            class="submit-button"
            :disabled="!formValido"
          >
            Entre
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
definePageMeta({
  layout: 'auth'
})

const email = ref('')
const senha = ref('')
const mostrarSenha = ref(false)

const erroEmail = ref('')
const erroLogin = ref('')

const formValido = computed(() =>
  email.value.trim() !== '' &&
  senha.value.trim() !== ''
)

async function entrar() {
  erroEmail.value = ''
  erroLogin.value = ''

  if (!email.value.trim()) {
    erroEmail.value = 'E-mail obrigatório'
    return
  }

  if (!senha.value.trim()) {
    erroLogin.value = 'Informe sua senha.'
    return
  }

  if (
    email.value === 'admin@gmail.com' &&
    senha.value === '123'
  ) {
    await navigateTo('/agenda')
    return
  }

  erroLogin.value = 'E-mail ou senha inválidos.'
}
</script>

<style scoped>

.auth-page {

  position:relative;

  width:100vw;

  min-height:100vh;

  overflow:hidden;

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

  margin-top:10px;

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