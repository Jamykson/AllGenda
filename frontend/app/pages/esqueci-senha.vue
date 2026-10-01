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
          Redefinir senha
        </h2>


        <p>
          Informe seus dados para criar uma nova senha.
        </p>

      </div>




      <form
        class="form"
        @submit.prevent="redefinir"
      >


        <input
          v-model="email"
          type="email"
          placeholder="E-mail"
          class="field"
        />



        <div class="password-wrapper">

          <input
            v-model="novaSenha"
            :type="mostrarSenha ? 'text':'password'"
            placeholder="Nova senha"
            class="field"
          />


          <button
            type="button"
            class="password-toggle"
            :aria-label="mostrarSenha ? 'Ocultar nova senha' : 'Mostrar nova senha'"
            @click="mostrarSenha=!mostrarSenha"
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



        <div class="password-wrapper">

          <input
            v-model="confirmarSenha"
            :type="mostrarConfirmar ? 'text':'password'"
            placeholder="Confirmar nova senha"
            class="field"
          />


          <button
            type="button"
            class="password-toggle"
            :aria-label="mostrarConfirmar ? 'Ocultar confirmação da senha' : 'Mostrar confirmação da senha'"
            @click="mostrarConfirmar=!mostrarConfirmar"
          >
            <svg
              v-if="!mostrarConfirmar"
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



        <button
          class="submit-button"
          type="submit"
        >

          Salvar nova senha

        </button>


      </form>




      <NuxtLink
        to="/"
        class="forgot-link"
      >

        Voltar para login

      </NuxtLink>



    </section>

  </main>


</div>

</template>



<script setup lang="ts">


definePageMeta({
  layout:'auth'
})


const email = ref('')
const novaSenha = ref('')
const confirmarSenha = ref('')


const mostrarSenha = ref(false)

const mostrarConfirmar = ref(false)



async function redefinir(){

  await navigateTo('/')

}


</script>



<style scoped>


.auth-page{

width:100vw;

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
#eff6ff,
#ffffff
);

color:#0f172a;

}



.auth-shell{

min-height:100vh;

display:flex;

justify-content:center;

align-items:flex-start;

padding:70px 24px;

}



.auth-content{

width:100%;

max-width:400px;

}



.brand{

text-align:center;

margin-bottom:18px;

}



.brand-name{

font-size:22px;

font-weight:800;

}



.headline{

text-align:center;

margin-bottom:25px;

}



.headline h2{

font-size:22px;

font-weight:800;

margin:0;

}



.headline p{

font-size:13px;

color:#64748b;

}



.form{

display:flex;

flex-direction:column;

gap:14px;

}



.field{

height:46px;

width:100%;

border:1px solid #cbd5e1;

border-radius:9px;

padding:0 14px;

font-size:14px;

outline:none;

background:white;

}



.field:focus{

border-color:#2563eb;

}



.password-wrapper{

position:relative;

}



.password-toggle{

position:absolute;

right:14px;

top:50%;

transform:translateY(-50%);

border:none;

background:none;

color:#2563eb;

cursor:pointer;

}



.submit-button{

height:46px;

border:none;

border-radius:9px;

background:#2563eb;

color:white;

font-weight:700;

font-size:14px;

cursor:pointer;

}



.submit-button:hover{

background:#1d4ed8;

}



.forgot-link{

display:block;

text-align:center;

margin-top:18px;

font-size:13px;

color:#2563eb;

text-decoration:none;

}



</style>