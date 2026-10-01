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

  icone?:string

  cor?:string

}


interface Aula {

  id:string

  data:string

  horario:string

  topico?:string

  disciplinaId:string

  disciplinaNome:string

  horaInicio?:string

  horaFim?:string

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

const alturaHora = 72



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

const data = new Date(`${chave}T00:00:00`)
const aulasDoBackend = aulas.value.filter(

aula=>
aula.data===chave

)

const aulasRecorrentes = Object.entries(horariosRecorrentes.value).flatMap(
  ([disciplinaId, horario]) => {
    const disciplina = disciplinas.value.find(item => item.id === disciplinaId)
    const dataDentroDoPeriodo = chave >= horario.dataInicio && chave <= horario.dataFim

    if (
      !disciplina
      || !Array.isArray(horario.dias)
      || !horario.dias.includes(data.getDay())
      || !dataDentroDoPeriodo
      || !horario.horaInicio
      || !horario.horaFim
    ) {
      return []
    }

    const personalizacao = personalizacoes.value[disciplinaId]
    const horarioFormatado = `${horario.horaInicio} – ${horario.horaFim}`
    const jaExiste = aulasDoBackend.some(aula =>
      aula.disciplinaId === disciplinaId
      && aula.horario === horarioFormatado
    )

    if (jaExiste) return []

    return [{
      id: `recorrente-${disciplinaId}-${chave}`,
      data: chave,
      horario: horarioFormatado,
      disciplinaId,
      disciplinaNome: personalizacao?.nome || disciplina.nome,
      horaInicio: horario.horaInicio,
      horaFim: horario.horaFim,
      recorrente: true,
      cor: personalizacao?.cor || '#2563eb',
      icone: personalizacao?.icone
    }]
  }
)

return [...aulasDoBackend, ...aulasRecorrentes]

}


function estiloEventoSemana(aula: Aula) {
  const corFundo = aula.cor || '#60a5fa'
  const estilo: Record<string, string> = {
    background: corFundo,
    color: corTextoParaFundo(corFundo)
  }

  if (!aula.horaInicio || !aula.horaFim) return estilo

  const [horaInicio, minutoInicio] = aula.horaInicio.split(':').map(Number)
  const [horaFim, minutoFim] = aula.horaFim.split(':').map(Number)
  const inicioMinutos = horaInicio * 60 + minutoInicio
  const fimMinutos = horaFim * 60 + minutoFim

  if (!Number.isFinite(inicioMinutos) || !Number.isFinite(fimMinutos)) return estilo

  const inicioVisivel = horaInicial * 60
  const fimVisivel = horaFinal * 60
  const inicio = Math.max(inicioMinutos, inicioVisivel)
  const fim = Math.min(fimMinutos, fimVisivel)

  if (fim <= inicio) return estilo

  estilo.top = `${((inicio - inicioVisivel) / 60) * alturaHora}px`
  estilo.height = `${Math.max(44, ((fim - inicio) / 60) * alturaHora)}px`
  return estilo
}


function corTextoParaFundo(corFundo: string) {
  const correspondencia = /^#?([\da-f]{2})([\da-f]{2})([\da-f]{2})$/i.exec(corFundo)
  if (!correspondencia) return '#ffffff'

  const [vermelho, verde, azul] = correspondencia.slice(1)
    .map(canal => Number.parseInt(canal, 16) / 255)
  const maior = Math.max(vermelho ?? 0, verde ?? 0, azul ?? 0)
  const menor = Math.min(vermelho ?? 0, verde ?? 0, azul ?? 0)
  const diferenca = maior - menor
  const luminosidade = (maior + menor) / 2

  if (diferenca === 0) {
    return luminosidade > 0.5 ? '#0f172a' : '#f8fafc'
  }

  let matiz = 0
  if (maior === vermelho) {
    matiz = 60 * (((verde ?? 0) - (azul ?? 0)) / diferenca % 6)
  }
  else if (maior === verde) {
    matiz = 60 * (((azul ?? 0) - (vermelho ?? 0)) / diferenca + 2)
  }
  else {
    matiz = 60 * (((vermelho ?? 0) - (verde ?? 0)) / diferenca + 4)
  }

  if (matiz < 0) matiz += 360

  const saturacao = diferenca / (1 - Math.abs(2 * luminosidade - 1))
  const luminosidadeTexto = luminosidade > 0.48 ? 22 : 82

  return `hsl(${Math.round(matiz)}, ${Math.round(saturacao * 100)}%, ${luminosidadeTexto}%)`
}


function carregarRegistroLocal<T>(chave: string): Record<string, T> {
  try {
    const registro = JSON.parse(localStorage.getItem(chave) || '{}')
    return registro && typeof registro === 'object' && !Array.isArray(registro)
      ? registro as Record<string, T>
      : {}
  }
  catch {
    return {}
  }
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




function irParaHoje() {

  dataAtual.value = new Date()

}


async function carregarAgenda() {

  carregando.value = true

  erro.value = ''

  horariosRecorrentes.value = carregarRegistroLocal<HorarioRecorrente>(CHAVE_HORARIOS)
  personalizacoes.value = carregarRegistroLocal<PersonalizacaoDisciplina>(CHAVE_PERSONALIZACOES)

  try {

    const respostaDisciplinas =
      await $api.get('/api/disciplinas')


    disciplinas.value =
      respostaDisciplinas.data


    const respostasAulas =
      await Promise.all(

        disciplinas.value.map(
          async (disciplina) => {

            const resposta =
              await $api.get(
                '/api/aulas',
                {
                  params: {
                    disciplinaId:
                      disciplina.id
                  }
                }
              )


            return resposta.data.map(
              (aula: any) => ({

                ...aula,

                disciplinaId:
                  disciplina.id,

                disciplinaNome:
                  disciplina.nome,

                cor:
                  '#2563eb'

              })
            )

          }
        )

      )


    aulas.value =
      respostasAulas.flat()

  }
  catch (e) {

    console.error(
      '[agenda] erro:',
      e
    )

    erro.value =
      'Não foi possível carregar a agenda.'

  }
  finally {

    carregando.value = false

  }

}


onMounted(() => {

  carregarAgenda()

})


</script>


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
aula.cor || '#2563eb',

color:
corTextoParaFundo(aula.cor || '#2563eb')

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

  <div class="fuso">
    GMT-03
  </div>


  <div
    v-for="dia in diasSemanaAtual"
    :key="dia.chave"
    class="dia-semana"
  >

    <strong>
      {{ dia.nome }}
    </strong>


    <span
      :class="{
        'bolinha-hoje': dia.hoje
      }"
    >
      {{ dia.numero }}
    </span>

  </div>

</div>



  <div class="corpo-semana">


    <div class="coluna-horas">

      <div
        v-for="hora in horas"
        :key="hora"
        class="hora"
      >

        {{ hora }}:00

      </div>

    </div>



    <div
      v-for="dia in diasSemanaAtual"
      :key="dia.chave"
      class="coluna-dia"
    >


      <div
        v-for="hora in horas"
        :key="hora"
        class="linha-hora"
      >

      </div>



      <div
        v-for="aula in aulasDoDia(dia.chave)"
        :key="aula.id"
        class="evento-semana"
        :style="{
          ...estiloEventoSemana(aula)
        }"
      >

        <strong>
          {{ aula.disciplinaNome }}
        </strong>


        <span>
          {{ aula.horario }}
        </span>


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
.agenda-container {
  width: 100%;
  height: 100vh;
  padding: 0 14px 10px 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* HEADER */
.agenda-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 54px;
  margin-bottom: 16px;
  padding: 0 4px;
  gap: 16px;
  flex-shrink: 0;
}

.controle-data {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.controle-data h1 {
  font-size: 16px;
  font-weight: 700;
  text-transform: capitalize;
  margin-left: 12px;
  color: #0f172a;
}

.btn-hoje,
.btn-navegacao {
  height: 36px;
  border: 1px solid #dbe3ef;
  background: white;
  border-radius: 6px;
  padding: 0 16px;
  cursor: pointer;
  font-weight: 600;
  color: #0f172a;
}

.btn-navegacao {
  width: 28px;
  padding: 0;
  font-size: 20px;
  border-color: transparent;
  color: #64748b;
}

.controle-visao {
  display: flex;
  gap: 4px;
  background: #f1f5f9;
  padding: 3px;
  border-radius: 8px;
}

.btn-visao {
  border: none;
  background: transparent;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  color: #64748b;
}

.btn-visao.ativo {
  background: white;
  color: #2563eb;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.16);
}

/* CALENDARIO MÊS */
.calendario-mes {
  width: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
}

.cabecalho-dias {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background: #f8fafc;
}

.nome-dia {
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
}

.grade-mes {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  grid-template-rows: repeat(6, 1fr);
}

.celula-dia {
  min-height: 0;
  padding: 8px;
  border-top: 1px solid #e2e8f0;
  border-right: 1px solid #e2e8f0;
  background: white;
  overflow: hidden;
  cursor: pointer;
  transition: background-color 160ms ease, box-shadow 160ms ease;
}

.celula-dia:nth-child(7n) {
  border-right: none;
}

.celula-dia.fora-mes {
  background: #f8fafc;
  color: #94a3b8;
}

.celula-dia.hoje {
  background: #eff6ff;
}

.celula-dia:hover {
  background: #f1f7ff;
  box-shadow: inset 0 0 0 1px #93c5fd;
}

.numero-dia {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 11px;
  margin: 0 0 6px auto;
  border-radius: 50%;
  color: #475569;
  transition: transform 160ms ease;
}

.celula-dia:hover .numero-dia {
  transform: scale(1.12);
}

.celula-dia.hoje .numero-dia {
  background: #2563eb;
  color: #ffffff;
}

.eventos-dia {
  display: flex;
  flex-direction: column;
  gap: 6px;
  overflow: hidden;
}

.evento {
  border-radius: 8px;
  padding: 6px 8px;
  color: white;
  font-size: 12px;
}

/* SEMANA - TEMA CLARO */
.calendario-semana {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}

.cabecalho-semana {
  display: grid;
  grid-template-columns: 70px repeat(7, 1fr);
  background: #ffffff;
  border-bottom: 1px solid #e2e8f0;
}

.fuso {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 8px;
  background: #f8fafc;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
  border-right: 1px solid #e2e8f0;
}

.dia-semana {
  min-height: 78px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: #ffffff;
  border-right: 1px solid #eef2f7;
  cursor: pointer;
  transition: background-color 160ms ease;
}

.dia-semana:hover {
  background: #f1f7ff;
}

.dia-semana:last-child {
  border-right: none;
}

.dia-semana strong {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.dia-semana span {
  width: 40px;
  height: 40px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  transition: transform 160ms ease;
}

.dia-semana:hover span {
  transform: scale(1.08);
}

.bolinha-hoje {
  background: #bfdbfe;
  color: #1d4ed8 !important;
}

.corpo-semana {
  flex: 1;
  display: grid;
  grid-template-columns: 70px repeat(7, 1fr);
  min-height: 0;
  overflow: auto;
  background: #ffffff;
}

.coluna-horas {
  position: sticky;
  left: 0;
  z-index: 3;
  background: #f8fafc;
  border-right: 1px solid #e2e8f0;
}

.hora {
  height: 72px;
  padding: 8px 6px 0;
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  border-bottom: 1px solid #edf2f7;
  box-sizing: border-box;
  cursor: pointer;
  transition: background-color 160ms ease, color 160ms ease;
}

.hora:hover {
  background: #eaf2ff;
  color: #1d4ed8;
}

.coluna-dia {
  position: relative;
  background: #ffffff;
  border-right: 1px solid #e2e8f0;
}

.coluna-dia:last-child {
  border-right: none;
}

.linha-hora {
  height: 72px;
  border-bottom: 1px solid #edf2f7;
  box-sizing: border-box;
  cursor: pointer;
  transition: background-color 160ms ease, box-shadow 160ms ease;
}

.linha-hora:hover {
  background: #f1f7ff;
  box-shadow: inset 0 0 0 1px #bfdbfe;
}

.evento-semana {
  position: absolute;
  left: 8px;
  right: 8px;
  min-height: 44px;
  border-radius: 12px;
  padding: 8px 10px;
  background: #dbeafe;
  border: 1px solid #93c5fd;
  color: #1e3a8a;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.12);
  overflow: hidden;
}

.evento-semana strong {
  display: block;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.2;
}

.evento-semana span {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: inherit;
}

/* ERRO */
.erro {
  margin-top: 20px;
  padding: 14px;
  background: #fee2e2;
  color: #991b1b;
  border-radius: 10px;
}

/* RESPONSIVIDADE */
@media (max-width: 900px) {
  .agenda-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .controle-data h1 {
    font-size: 22px;
  }

  .celula-dia {
    min-height: 80px;
    padding: 8px;
  }
}

@media (max-width: 600px) {
  .agenda-container {
    width: 100%;
    height: 100vh;
    padding: 16px;
    background: #ffffff;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  .grade-mes {
    min-width: 700px;
  }

  .calendario-mes {
    overflow-x: auto;
  }

  .calendario-semana {
    overflow: auto;
  }

  .cabecalho-semana,
  .corpo-semana {
    min-width: 760px;
  }

  .btn-visao {
    padding: 8px 12px;
  }

  .controle-data h1 {
    width: 100%;
    margin-left: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .celula-dia,
  .numero-dia,
  .dia-semana,
  .dia-semana span,
  .hora,
  .linha-hora {
    transition: none;
  }

  .celula-dia:hover .numero-dia,
  .dia-semana:hover span {
    transform: none;
  }
}
</style>