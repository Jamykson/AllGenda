<template>

<aside class="sidebar">


<div class="logo">
    <strong>AllGenda</strong>
    <span>Organização acadêmica</span>
</div>


<nav>

<NuxtLink to="/agenda">
    <span class="nav-icon" aria-hidden="true">▣</span>
  Agenda
</NuxtLink>


<NuxtLink to="/disciplinas">
    <span class="nav-icon" aria-hidden="true">▤</span>
  Disciplinas
</NuxtLink>


</nav>



<div class="rodape">
    <span class="nav-icon" aria-hidden="true">↪</span>
  Sair
</div>


</aside>

</template>



<style scoped>

.sidebar {

    width:180px;

    min-width:180px;

    flex-shrink:0;

    height:100vh;

    background:#0f172a;

    border-right:1px solid #1e293b;

    display:flex;

    flex-direction:column;

    padding:14px 6px 12px;

}



.logo {

    display:flex;

    flex-direction:column;

    gap:6px;

    font-size:22px;

    font-weight:800;

    margin:0 -6px 12px;

    padding:0 14px 18px;

    color:#ffffff;

    border-bottom:1px solid #1e293b;

    letter-spacing:0;

}

.logo span {

    color:#94a3b8;

    font-size:11px;

    font-weight:400;

}



nav {

    display:flex;

    flex-direction:column;

    gap:6px;

}



nav a {

    display:flex;

    align-items:center;

    gap:10px;

    padding:10px 12px;

    border-radius:6px;

    text-decoration:none;

    color:#94a3b8;

    font-size:15px;

    font-weight:600;

    transition:.2s;

}



nav a:hover {

    background:#1e293b;

}



nav a.router-link-active {

    background:#2563eb;

    color:#ffffff;

}

.nav-icon {

    width:12px;

    flex:0 0 12px;

    font-size:12px;

    text-align:center;

}



.rodape {

    margin-top:auto;

    margin-right:-6px;

    margin-left:-6px;

    padding:22px 12px 10px;

    color:#94a3b8;

    border-top:1px solid #1e293b;

    display:flex;

    align-items:center;

    gap:10px;

    font-size:15px;

    cursor:pointer;

}



@media(max-width:768px){


.sidebar {

    width:100%;

    min-width:100%;

    height:auto;

    flex-direction:row;

    align-items:center;

    justify-content:space-between;

    padding:12px 16px;

}


.logo {

    margin-bottom:0;

}


nav {

    flex-direction:row;

}


.rodape {

    margin-top:0;

}


}


</style>