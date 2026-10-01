<template>
  <div class="auth-page">

    <main class="auth-shell">
      <section class="auth-content">

        <div class="brand">
          <h1 class="brand-name">
            AllGenda
          </h1>
        </div>

        <div class="headline">
          <h2>
            Você pode criar uma conta em segundos!
          </h2>

          <p>
            Já tem uma conta?

            <NuxtLink to="/">
              Entrar
            </NuxtLink>
          </p>
        </div>

        <form
          class="form"
          @submit.prevent="cadastrar"
        >
          <input
            v-model="nome"
            type="text"
            placeholder="Nome completo"
            class="field"
          />

          <input
            v-model="email"
            type="email"
            placeholder="E-mail"
            class="field"
          />

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
                width="17"
                height="17"
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
                width="17"
                height="17"
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
            v-if="erro"
            class="form-error"
          >
            {{ erro }}
          </p>

          <button
            type="submit"
            class="submit-button"
            :disabled="!formValido"
          >
            Cadastrar-se com e-mail
          </button>
        </form>

      </section>
    </main>

    <footer class="auth-footer">
      Ao continuar, você concorda com nossos
      <a href="#">Termos de serviço</a>
      e
      <a href="#">Política de privacidade</a>.
      <a href="#">Precisa de ajuda?</a>
    </footer>

  </div>
</template>

<script setup lang="ts">
definePageMeta({
  layout: 'auth'
})

const nome = ref('')
const email = ref('')
const senha = ref('')
const mostrarSenha = ref(false)
const erro = ref('')

const formValido = computed(() =>
  nome.value.trim() !== '' &&
  email.value.trim() !== '' &&
  senha.value.trim() !== ''
)

async function cadastrar() {
  erro.value = ''

  if (!formValido.value) {
    erro.value = 'Preencha todos os campos.'
    return
  }

  await navigateTo('/disciplinas')
}
</script>

<style scoped>
.auth-page {
  position: relative;

  width: 100vw;
  min-height: 100vh;

  overflow-x: hidden;

  background:
    radial-gradient(
      circle at 15% 0%,
      rgba(147, 197, 253, 0.42) 0%,
      rgba(219, 234, 254, 0.22) 25%,
      transparent 48%
    ),
    radial-gradient(
      circle at 85% 0%,
      rgba(96, 165, 250, 0.32) 0%,
      rgba(219, 234, 254, 0.18) 28%,
      transparent 50%
    ),
    linear-gradient(
      180deg,
      #eff6ff 0%,
      #ffffff 38%,
      #ffffff 100%
    );

  color: #0f172a;
}

.auth-shell {
  min-height: calc(100vh - 48px);

  display: flex;

  align-items: flex-start;
  justify-content: center;

  padding: 68px 24px 24px;
}

.auth-content {
  width: 100%;
  max-width: 400px;

  position: relative;
  z-index: 2;
}

.brand {
  text-align: center;
  margin-bottom: 14px;
}

.brand-name {
  margin: 0;

  font-size: 22px;
  line-height: 1;

  font-weight: 800;
  letter-spacing: -0.7px;
}

.headline {
  text-align: center;
  margin-bottom: 16px;
}

.headline h2 {
  margin: 0;

  font-size: 18px;
  line-height: 1.35;

  font-weight: 800;
  letter-spacing: -0.3px;
}

.headline p {
  margin: 5px 0 0;

  font-size: 12px;

  color: #64748b;
}

.headline a,
.auth-footer a {
  color: #2563eb;
  text-decoration: none;
}

.headline a:hover,
.auth-footer a:hover {
  text-decoration: underline;
}

.form {
  display: flex;
  flex-direction: column;

  gap: 10px;
}

.field {
  width: 100%;
  height: 38px;

  border: 1px solid #cbd5e1;
  border-radius: 7px;

  background: rgba(255,255,255,.97);

  padding: 0 10px;

  outline: none;

  color: #0f172a;

  font-size: 12px;

  transition: .2s ease;
}

.field::placeholder {
  color: #94a3b8;
}

.field:focus {
  border-color: #3b82f6;

  box-shadow:
    0 0 0 2px rgba(59,130,246,.09);
}

.password-wrapper {
  position: relative;
}

.field-password {
  padding-right: 38px;
}

.password-toggle {
  position: absolute;

  right: 9px;
  top: 50%;

  transform: translateY(-50%);

  border: 0;

  background: transparent;

  color: #64748b;

  cursor: pointer;

  padding: 3px;

  display: flex;
  align-items: center;
  justify-content: center;
}

.form-error {
  margin: -3px 0 0;

  font-size: 11px;

  color: #dc2626;
}

.submit-button {
  width: 100%;
  height: 38px;

  border: none;
  border-radius: 7px;

  background: #2563eb;

  color: white;

  font-size: 12px;
  font-weight: 700;

  cursor: pointer;

  transition: .2s ease;
}

.submit-button:hover:not(:disabled) {
  background: #1d4ed8;
}

.submit-button:disabled {
  background: #cbd5e1;

  color: white;

  cursor: not-allowed;
}

.auth-footer {
  position: relative;
  z-index: 2;

  padding: 0 20px 20px;

  text-align: center;

  font-size: 10px;

  color: #64748b;
}

@media (max-width: 640px) {
  .auth-shell {
    padding:
      35px
      18px
      20px;
  }

  .auth-content {
    max-width: 360px;
  }

  .headline h2 {
    font-size: 17px;
  }
}
</style>