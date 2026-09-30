<template>

<aside class="sidebar">


<div class="logo">
  AllGenda
</div>



<nav>

<NuxtLink to="/agenda">
  Agenda
</NuxtLink>


<NuxtLink to="/disciplinas">
  Disciplinas
</NuxtLink>


</nav>



<div class="rodape">
  Sair
</div>


</aside>


</template>



<style scoped>


.sidebar {

width:260px;

height:100vh;

background:#ffffff;

border-right:1px solid #e5e7eb;

display:flex;

flex-direction:column;

padding:24px;

}



.logo {

font-size:24px;

font-weight:800;

margin-bottom:40px;

color:#2563eb;

}



nav {

display:flex;

flex-direction:column;

gap:8px;

}



nav a {

padding:12px 14px;

border-radius:10px;

text-decoration:none;

color:#475569;

font-weight:600;

}



nav a.router-link-active {

background:#eff6ff;

color:#2563eb;

}



.rodape {

margin-top:auto;

color:#64748b;

cursor:pointer;

}



</style>