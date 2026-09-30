<script setup lang="ts">

import {useDisciplinas} from "~/composables/useDisciplinas"


const props = defineProps({

disciplina:Object

})


const {atualizarCor}=useDisciplinas()


const cores=[

"#2563eb",
"#7c3aed",
"#db2777",
"#dc2626",
"#ea580c",
"#16a34a",
"#0891b2",
"#334155"

]


async function mudar(cor:string){

await atualizarCor(
props.disciplina.id,
cor
)


props.disciplina.cor=cor


}


</script>



<template>


<div>


<h4>Cor</h4>


<div class="flex gap-3 mt-3">


<button

v-for="c in cores"

:key="c"

@click="mudar(c)"

class="w-8 h-8 rounded-full"

:style="{background:c}"

></button>


</div>


</div>


</template>