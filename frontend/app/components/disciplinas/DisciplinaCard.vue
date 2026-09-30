<script setup lang="ts">

import type { Disciplina } from "~/types/disciplina"


defineProps<{

disciplina:Disciplina

}>()


</script>


<template>

<div

class="border rounded-xl p-5 cursor-pointer hover:shadow"

>

<div class="flex gap-4 items-center">


<div

class="w-12 h-12 rounded-xl flex items-center justify-center text-xl"

:style="{
background: disciplina.cor || '#dbeafe'
}"

>

{{ disciplina.icone || disciplina.nome[0] }}

</div>


<div>

<h3 class="font-bold">

{{ disciplina.nome }}

</h3>


<p class="text-sm text-gray-500">

Clique para configurar

</p>


</div>


</div>


</div>


</template>