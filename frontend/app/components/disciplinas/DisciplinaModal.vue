<script setup lang="ts">

import type { Disciplina } from "~/types/disciplina"


defineProps<{

disciplina:Disciplina

}>()


const emit = defineEmits([

"fechar"

])


</script>



<template>


<div class="fixed inset-0 bg-black/40 flex items-center justify-center">


<div class="bg-white rounded-2xl w-[700px] p-8">


<div class="flex justify-between">


<h2 class="text-xl font-bold">

{{ disciplina.nome }}

</h2>


<button @click="emit('fechar')">

✕

</button>


</div>



<div class="mt-6 flex gap-3">


<EditarNome
:disciplina="disciplina"
/>


<EditarCor
:disciplina="disciplina"
/>


<EditarIcone
:disciplina="disciplina"
/>


</div>



<hr class="my-6">


<HorarioModal/>


</div>


</div>


</template>