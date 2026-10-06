<script setup lang="ts">

import type { Disciplina, NovaDisciplina } from '~/types/disciplina'
import { Plus } from '@lucide/vue'

const { listar, criar, excluir } = useDisciplinas()

const disciplinas = ref<Disciplina[]>([])
const selecionada = ref<Disciplina | null>(null)
const disciplinaComAulas = ref<Disciplina | null>(null)
const criando = ref(false)
const salvando = ref(false)
const carregando = ref(true)
const erroLista = ref('')
const erroCriacao = ref('')
const erroExclusao = ref('')
const versaoCards = ref(0)

onMounted(async () => {
  try {
    disciplinas.value = await listar()
  }
  catch (error) {
    console.error('[disciplinas] erro ao carregar:', error)
    erroLista.value = 'Não foi possível carregar as disciplinas.'
  }
  finally {
    carregando.value = false
  }
})

async function salvarNovaDisciplina(nova: NovaDisciplina) {
  salvando.value = true
  erroCriacao.value = ''

  try {
    const criada = await criar(nova)
    disciplinas.value.push(criada)
    criando.value = false
  }
  catch (error) {
    console.error('[disciplinas] erro ao criar:', error)
    erroCriacao.value = 'Não foi possível salvar a disciplina. Tente novamente.'
  }
  finally {
    salvando.value = false
  }
}

async function excluirDisciplina(disciplina: Disciplina) {
  const confirmar = window.confirm(
    `Excluir a disciplina "${disciplina.nome}" e suas aulas? Essa ação não pode ser desfeita.`
  )

  if (!confirmar) return

  erroExclusao.value = ''

  try {
    await excluir(disciplina.id)
    disciplinas.value = disciplinas.value.filter(item => item.id !== disciplina.id)
    if (selecionada.value?.id === disciplina.id) {
      selecionada.value = null
    }
    if (disciplinaComAulas.value?.id === disciplina.id) {
      disciplinaComAulas.value = null
    }
  }
  catch (error) {
    console.error('[disciplinas] erro ao excluir:', error)
    erroExclusao.value = 'Não foi possível excluir a disciplina. Tente novamente.'
  }
}

function fecharDetalhes() {
  selecionada.value = null
  versaoCards.value += 1
}

</script>


<template>

<div class="pagina-disciplinas">

  <div class="header-pagina">
    <h1>Disciplinas</h1>

    <button
      type="button"
      class="btn-nova"
      @click="criando = true; erroCriacao = ''"
    >
      <Plus :size="16" :stroke-width="2" aria-hidden="true" />
      Nova disciplina
    </button>
  </div>

  <p v-if="erroLista" class="mensagem-erro" role="alert">
    {{ erroLista }}
  </p>

  <p v-if="erroExclusao" class="mensagem-erro" role="alert">
    {{ erroExclusao }}
  </p>

  <p v-else-if="carregando" class="estado-lista" role="status">
    Carregando disciplinas...
  </p>

  <div v-else-if="disciplinas.length" class="grade-disciplinas">
    <DisciplinaCard
      v-for="disciplina in disciplinas"
      :key="`${disciplina.id}-${versaoCards}`"
      :disciplina="disciplina"
      @abrir="disciplinaComAulas = disciplina"
      @editar="selecionada = disciplina"
      @excluir="excluirDisciplina(disciplina)"
    />
  </div>

  <div v-else class="estado-vazio">
    <h2>Nenhuma disciplina cadastrada</h2>
    <p>Crie sua primeira disciplina para organizar suas aulas.</p>
    <button type="button" class="btn-nova" @click="criando = true">
      <Plus :size="16" :stroke-width="2" aria-hidden="true" />
      Nova disciplina
    </button>
  </div>

  <CriarDisciplinaModal
    v-if="criando"
    :salvando="salvando"
    :erro="erroCriacao"
    @cancelar="criando = false"
    @salvar="salvarNovaDisciplina"
  />

  <DisciplinaModal
    v-if="selecionada"
    :disciplina="selecionada"
    @fechar="fecharDetalhes"
  />

  <AulasDisciplinaModal
    v-if="disciplinaComAulas"
    :disciplina="disciplinaComAulas"
    @fechar="disciplinaComAulas = null"
  />
</div>


</template>


<style scoped>

.pagina-disciplinas {

  width: 100%;

}


.header-pagina {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}


.header-pagina h1 {

  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
}

.btn-nova {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 40px;
  padding: 0 12px;
  border: 0;
  border-radius: 7px;
  background: #2563eb;
  color: #fff;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 150ms ease;
}

.btn-nova:hover {
  background: #1d4ed8;
}

.grade-disciplinas {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 270px), 1fr));
  gap: 10px;
}

.estado-vazio {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 36px 20px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  color: #64748b;
  text-align: center;
}

.estado-vazio h2 {
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}

.estado-vazio .btn-nova {
  margin-top: 8px;
}

.estado-lista,
.mensagem-erro {
  padding: 12px 0;
  color: #64748b;
}

.mensagem-erro {
  color: #b91c1c;
}

@media (max-width: 600px) {
  .header-pagina {
    align-items: center;
  }

  .btn-nova {
    min-height: 36px;
  }
}


</style>