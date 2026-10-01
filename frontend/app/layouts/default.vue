<script setup lang="ts">
const route = useRoute()
</script>

<template>

<div class="app">

    <Sidebar />

    <main
        class="conteudo"
        :class="{ 'conteudo-agenda': route.path === '/agenda' }"
    >

        <slot />

    </main>

</div>

</template>


<style scoped>

.app {

    display:flex;

    min-height:100vh;

    width:100%;

    background:#f8fafc;

}


/* Área onde entram as páginas */

.conteudo {

    flex:1;

    min-width:0;

    padding:32px;

    display:flex;

    justify-content:center;

    align-items:flex-start;

}

.conteudo-agenda {
    padding: 0;
    justify-content: flex-start;
    align-items: stretch;
    overflow: hidden;
}


/* Responsividade */

@media(max-width:768px){

    .app{

        flex-direction:column;

    }


    .conteudo{

        padding:16px;

    }

    .conteudo.conteudo-agenda {
        padding: 0;
    }

}

</style>