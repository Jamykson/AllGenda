<script setup lang="ts">

import type { Disciplina } from '~/types/disciplina'

const { listar } = useDisciplinas()

const disciplinas = ref<Disciplina[]>([])

const selecionada = ref<Disciplina | null>(null)


onMounted(async () => {
  disciplinas.value = await listar()
})


</script>


<template>

<div class="pagina-disciplinas">

  <div class="header-pagina">
    <h1>Disciplinas</h1>

    <p>
      Gerencie suas disciplinas cadastradas
    </p>
  </div>


  <div class="grid grid-cols-1 md:grid-cols-3 gap-5">


    <DisciplinaCard

      v-for="disciplina in disciplinas"

      :key="disciplina.id"

      :disciplina="disciplina"

      @click="selecionada = disciplina"

    />


  </div>



  <DisciplinaModal

    v-if="selecionada"

    :disciplina="selecionada"

    @fechar="selecionada = null"

  />


</div>


</template>


<style scoped>

.pagina-disciplinas {

  width: 100%;

}


.header-pagina {

  margin-bottom: 24px;

}


.header-pagina h1 {

  font-size: 28px;
  font-weight: 700;

}


.header-pagina p {

  color: #64748b;

}



</style>