<script setup lang="ts">

definePageMeta({
  layout:'default'
})


interface Disciplina {
  id:string
  nome:string
}


interface HorarioRecorrente {

  dias:number[]

  horaInicio:string

  horaFim:string

  dataInicio:string

  dataFim:string

}


interface PersonalizacaoDisciplina {

  nome?:string

  icone:string

  cor:string

}


interface Aula {

  id:string

  data:string

  horario:string

  topico?:string

  disciplinaId:string

  disciplinaNome:string

  recorrente?:boolean

  cor?:string

  icone?:string

}



const { $api } = useNuxtApp()



const visualizacao =
ref<'mes'|'semana'>('mes')



const dataAtual =
ref(new Date())



const disciplinas =
ref<Disciplina[]>([])



const aulas =
ref<Aula[]>([])



const horariosRecorrentes =
ref<Record<string,HorarioRecorrente>>({})



const personalizacoes =
ref<Record<string,PersonalizacaoDisciplina>>({})



const carregando =
ref(true)



const erro =
ref('')



const CHAVE_HORARIOS =
'allgenda-horarios-recorrentes'



const CHAVE_PERSONALIZACOES =
'allgenda-personalizacoes-disciplinas'



const nomesDias = [

'Dom',

'Seg',

'Ter',

'Qua',

'Qui',

'Sex',

'Sáb'

]



const horaInicial = 7

const horaFinal = 24

const alturaHora = 64



const horas =
Array.from(

{
length:
horaFinal-horaInicial
},

(_,i)=>
horaInicial+i

)



const alturaSemana =
(horaFinal-horaInicial)*alturaHora




const tituloPeriodo =
computed(()=>{


if(
visualizacao.value==='mes'
){

return new Intl.DateTimeFormat(

'pt-BR',

{

month:'long',

year:'numeric'

}

).format(
dataAtual.value
)

}



const inicio =
inicioDaSemana(
dataAtual.value
)



const fim =
new Date(inicio)



fim.setDate(
fim.getDate()+6
)



return `${inicio.toLocaleDateString(
'pt-BR'
)}
-
${fim.toLocaleDateString(
'pt-BR'
)}`


})





function chaveData(data:Date){

const ano =
data.getFullYear()


const mes =
String(
data.getMonth()+1
).padStart(2,'0')


const dia =
String(
data.getDate()
).padStart(2,'0')


return `${ano}-${mes}-${dia}`

}





function mesmaData(
a:Date,
b:Date
){

return chaveData(a)===chaveData(b)

}





function inicioDaSemana(
data:Date
){

const inicio =
new Date(data)


inicio.setHours(
0,0,0,0
)


inicio.setDate(
inicio.getDate()-inicio.getDay()
)


return inicio

}





const diasCalendario =
computed(()=>{


const ano =
dataAtual.value.getFullYear()


const mes =
dataAtual.value.getMonth()



const primeiro =
new Date(
ano,
mes,
1
)



const inicio =
new Date(primeiro)



inicio.setDate(
primeiro.getDate() -
primeiro.getDay()
)



return Array.from(
{
length:42
},

(_,indice)=>{


const data =
new Date(inicio)



data.setDate(
inicio.getDate()+indice
)



return {

chave:
chaveData(data),

numero:
data.getDate(),

mesAtual:
data.getMonth()===mes,


hoje:
mesmaData(
data,
new Date()
)

}


}

)


})




const diasSemanaAtual =
computed(()=>{


const inicio =
inicioDaSemana(
dataAtual.value
)



return Array.from(
{
length:7
},

(_,indice)=>{


const data =
new Date(inicio)



data.setDate(
inicio.getDate()+indice
)



return {

chave:
chaveData(data),

numero:
data.getDate(),

nome:
nomesDias[indice],


hoje:
mesmaData(
data,
new Date()
)

}


}

)


})





function aulasDoDia(
chave:string
){

return aulas.value.filter(

aula=>
aula.data===chave

)

}





function anterior(){


const nova =
new Date(
dataAtual.value
)



if(
visualizacao.value==='mes'
){

nova.setMonth(
nova.getMonth()-1
)

}else{

nova.setDate(
nova.getDate()-7
)

}


dataAtual.value =
nova

}





function proximo(){


const nova =
new Date(
dataAtual.value
)



if(
visualizacao.value==='mes'
){

nova.setMonth(
nova.getMonth()+1
)

}else{

nova.setDate(
nova.getDate()+7
)

}


dataAtual.value =
nova

}




function irParaHoje(){

dataAtual.value =
new Date()

}


<template>

<div class="agenda-container">


<header class="agenda-header">


<div class="controle-data">


<button
class="btn-hoje"
@click="irParaHoje"
>
Hoje
</button>


<button
class="btn-navegacao"
@click="anterior"
>
‹
</button>


<button
class="btn-navegacao"
@click="proximo"
>
›
</button>



<h1>
{{ tituloPeriodo }}
</h1>


</div>



<div class="controle-visao">


<button

:class="[
'btn-visao',
{
ativo:
visualizacao==='semana'
}
]"

@click="visualizacao='semana'"

>
Semana
</button>



<button

:class="[
'btn-visao',
{
ativo:
visualizacao==='mes'
}
]"

@click="visualizacao='mes'"

>
Mês
</button>


</div>


</header>





<!-- =========================
        VISÃO MENSAL
========================= -->


<div

v-if="
visualizacao==='mes'
"

class="calendario-mes"



>


<div

class="cabecalho-dias"

>


<div

v-for="
dia in nomesDias
"

:key="dia"

class="nome-dia"

>

{{dia}}

</div>


</div>





<div

class="grade-mes"

>


<div

v-for="
dia in diasCalendario
"

:key="
dia.chave
"

class="celula-dia"

:class="{

'fora-mes':
!dia.mesAtual,

'hoje':
dia.hoje

}"

>


<div

class="numero-dia"

>

{{dia.numero}}

</div>





<div

class="eventos-dia"

>


<div

v-for="
aula in aulasDoDia(dia.chave)
"

:key="
aula.id
"

class="evento"

:style="{

background:
aula.cor || '#2563eb'

}"

>


<span>

{{aula.disciplinaNome}}

</span>



<small>

{{aula.horario}}

</small>



</div>


</div>


</div>


</div>


</div>







<!-- =========================
        VISÃO SEMANAL
========================= -->


<div

v-else

class="calendario-semana"

>



<div class="cabecalho-semana">


<div

class="coluna-hora"

>

</div>



<div

v-for="
dia in diasSemanaAtual
"

:key="
dia.chave
"

class="dia-semana"

>


<strong>

{{dia.nome}}

</strong>


<span

:class="{

'bolinha-hoje':
dia.hoje

}"

>

{{dia.numero}}

</span>


</div>



</div>








<div

class="corpo-semana"

>


<div

class="coluna-horas"

>


<div

v-for="
hora in horas
"

:key="
hora
"

class="hora"

>


{{hora}}:00


</div>


</div>






<div

v-for="
dia in diasSemanaAtual
"

:key="
dia.chave
"

class="coluna-dia"

>


<div

v-for="
hora in horas
"

:key="
hora
"

class="linha-hora"

>


</div>





<div

v-for="
aula in aulasDoDia(dia.chave)
"

:key="
aula.id
"

class="evento-semana"

:style="{

background:
aula.cor || '#2563eb'

}"

>


<strong>

{{aula.disciplinaNome}}

</strong>


<span>

{{aula.horario}}

</span>


<p

v-if="
aula.topico
"

>

{{aula.topico}}

</p>


</div>





</div>


</div>



</div>







<div

v-if="
carregando
"

class="loading"

>

Carregando agenda...

</div>



<div

v-if="
erro
"

class="erro"

>

{{erro}}

</div>



</div>


</template>

<style scoped>


.agenda-container{

width:100%;

height:100%;

padding:24px;

background:#f8fafc;

}





/* =========================
 HEADER
========================= */


.agenda-header{

display:flex;

justify-content:space-between;

align-items:center;

margin-bottom:24px;

}



.controle-data{

display:flex;

align-items:center;

gap:12px;

}



.controle-data h1{

font-size:24px;

font-weight:700;

text-transform:capitalize;

color:#111827;

margin-left:12px;

}



.btn-hoje,
.btn-navegacao{

background:white;

border:1px solid #e5e7eb;

border-radius:10px;

height:38px;

padding:0 16px;

cursor:pointer;

font-size:14px;

font-weight:600;

transition:.2s;

}



.btn-navegacao{

width:40px;

padding:0;

font-size:22px;

}



.btn-hoje:hover,
.btn-navegacao:hover{

background:#f1f5f9;

}






/* =========================
 BOTÕES VISÃO
========================= */


.controle-visao{

background:#e5e7eb;

padding:4px;

border-radius:12px;

display:flex;

gap:4px;

}



.btn-visao{

border:none;

background:transparent;

padding:8px 18px;

border-radius:9px;

cursor:pointer;

font-weight:600;

color:#475569;

}



.btn-visao.ativo{

background:white;

color:#2563eb;

box-shadow:0 2px 5px rgba(0,0,0,.08);

}






/* =========================
 CALENDÁRIO MENSAL
========================= */


.calendario-mes{

background:white;

border-radius:16px;

overflow:hidden;

border:1px solid #e5e7eb;

}



.cabecalho-dias{

display:grid;

grid-template-columns:repeat(7,1fr);

background:#f8fafc;

border-bottom:1px solid #e5e7eb;

}



.nome-dia{

padding:14px;

text-align:center;

font-size:13px;

font-weight:700;

color:#64748b;

}





.grade-mes{

display:grid;

grid-template-columns:repeat(7,1fr);

}



.celula-dia{

min-height:120px;

border-right:1px solid #e5e7eb;

border-bottom:1px solid #e5e7eb;

padding:10px;

background:white;

}



.celula-dia:nth-child(7n){

border-right:none;

}



.celula-dia.fora-mes{

background:#f8fafc;

color:#94a3b8;

}



.celula-dia.hoje{

background:#eff6ff;

}



.numero-dia{

font-size:14px;

font-weight:700;

margin-bottom:8px;

}





/* =========================
 EVENTOS
========================= */


.eventos-dia{

display:flex;

flex-direction:column;

gap:6px;

}



.evento{

color:white;

border-radius:8px;

padding:6px 8px;

font-size:12px;

display:flex;

flex-direction:column;

cursor:pointer;

}



.evento span{

font-weight:700;

}



.evento small{

opacity:.9;

}





/* =========================
 SEMANA
========================= */


.calendario-semana{

background:white;

border-radius:16px;

overflow:hidden;

border:1px solid #e5e7eb;

}



.cabecalho-semana{

display:grid;

grid-template-columns:70px repeat(7,1fr);

border-bottom:1px solid #e5e7eb;

}



.dia-semana{

padding:14px;

text-align:center;

border-left:1px solid #e5e7eb;

display:flex;

flex-direction:column;

gap:4px;

}



.dia-semana strong{

font-size:13px;

color:#64748b;

}



.dia-semana span{

font-size:18px;

font-weight:700;

}





.bolinha-hoje{

color:#2563eb;

}





.corpo-semana{

display:grid;

grid-template-columns:70px repeat(7,1fr);

height:calc(100vh - 250px);

overflow:auto;

}





.coluna-horas{

border-right:1px solid #e5e7eb;

}



.hora{

height:64px;

font-size:12px;

color:#94a3b8;

padding:8px;

text-align:right;

border-bottom:1px solid #f1f5f9;

}





.coluna-dia{

position:relative;

border-right:1px solid #e5e7eb;

}



.linha-hora{

height:64px;

border-bottom:1px solid #f1f5f9;

}





.evento-semana{

position:absolute;

left:8px;

right:8px;

top:10px;

border-radius:10px;

padding:8px;

color:white;

font-size:12px;

z-index:2;

}



.evento-semana strong{

display:block;

}



.evento-semana span{

font-size:11px;

}





/* =========================
 ESTADOS
========================= */


.loading{

margin-top:20px;

color:#64748b;

}



.erro{

margin-top:20px;

padding:12px;

background:#fee2e2;

color:#991b1b;

border-radius:10px;

}




</style>