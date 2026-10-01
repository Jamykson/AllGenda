<template>

<div class="auth-page">

  <main class="auth-shell">

    <section class="auth-content">


      <div class="brand">

        <div class="brand-mark">

          <span class="mark-part mark-part-1"></span>
          <span class="mark-part mark-part-2"></span>

        </div>


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
            @click="mostrarSenha=!mostrarSenha"
          >
            Ver
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
            @click="mostrarConfirmar=!mostrarConfirmar"
          >
            Ver
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



.brand-mark{

width:46px;

height:34px;

margin:auto;

position:relative;

}



.mark-part{

position:absolute;

width:30px;

height:11px;

border-radius:999px;

}



.mark-part-1{

left:2px;

top:7px;

transform:rotate(40deg);

background:
linear-gradient(
90deg,
#38bdf8,
#2563eb
);

}



.mark-part-2{

right:2px;

top:7px;

transform:rotate(-40deg);

background:
linear-gradient(
90deg,
#2563eb,
#1d4ed8
);

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