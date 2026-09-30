<template>

<div class="app">

    <Sidebar />

    <main class="conteudo">

        <slot />

    </main>


</div>

</template>


<style scoped>


.app{

display:flex;

min-height:100vh;

background:#f8fafc;

}


</style>