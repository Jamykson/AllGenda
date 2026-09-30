<script setup lang="ts">


import {useDisciplinas} from "~/composables/useDisciplinas"


const props = defineProps({

disciplina:Object

})


const {atualizarIcone}=useDisciplinas()



const icones=[

"💻",
"📚",
"🧮",
"🌎",
"🧠",
"📐"

]


async function mudar(icon:string){

await atualizarIcone(
props.disciplina.id,
icon
)


props.disciplina.icone=icon

}


</script>



<template>


<div>


<h4>Ícone</h4>


<div class="flex gap-3 mt-3">


<button

v-for="i in icones"

:key="i"

@click="mudar(i)"

class="border rounded p-2 text-xl"

>

{{i}}

</button>


</div>


</div>


</template>