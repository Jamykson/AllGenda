<script setup lang="ts">
import type { Avaliacao, TipoAvaliacao } from '~/types/avaliacao'
import type { Disciplina } from '~/types/disciplina'
import { mensagemDeErro } from '~/composables/useAnotacoes'

const api = useAvaliacoes()
const disciplinasApi = useDisciplinas()
const disciplinas = ref<Disciplina[]>([])
const disciplinaId = ref('')
const tipo = ref<TipoAvaliacao>('PROVA')
const descricao = ref('')
const data = ref('')
const avaliacoes = ref<Avaliacao[]>([])
const carregandoDisciplinas = ref(true)
const carregando = ref(false)
const salvando = ref(false)
const erroDisciplinas = ref('')
const erroLista = ref('')
const erroForm = ref('')
const sucesso = ref('')
let consulta = 0
const hoje = new Date().toLocaleDateString('en-CA')
const ordenadas = computed(() => [...avaliacoes.value].sort((a, b) => a.data.localeCompare(b.data) || a.id.localeCompare(b.id)))
const proximas = computed(() => ordenadas.value.filter(item => item.data >= hoje))
const anteriores = computed(() => ordenadas.value.filter(item => item.data < hoje).reverse())

function formatarData(valor: string) {
  return new Date(`${valor}T12:00:00`).toLocaleDateString('pt-BR')
}

async function carregarDisciplinas() {
  carregandoDisciplinas.value = true
  erroDisciplinas.value = ''
  try {
    disciplinas.value = await disciplinasApi.listar()
    if (!disciplinaId.value) disciplinaId.value = disciplinas.value[0]?.id ?? ''
  } catch (erro) {
    erroDisciplinas.value = mensagemDeErro(erro, 'Não foi possível carregar as disciplinas.')
  } finally {
    carregandoDisciplinas.value = false
  }
}

async function carregarAvaliacoes() {
  const atual = ++consulta
  const id = disciplinaId.value
  avaliacoes.value = []
  erroLista.value = ''
  if (!id) return
  carregando.value = true
  try {
    const resposta = await api.listar(id)
    // Uma resposta antiga não pode substituir a lista da disciplina recém-selecionada.
    if (atual === consulta) avaliacoes.value = resposta
  } catch (erro) {
    if (atual === consulta) erroLista.value = mensagemDeErro(erro, 'Não foi possível carregar as avaliações.')
  } finally {
    if (atual === consulta) carregando.value = false
  }
}

async function cadastrar() {
  if (salvando.value) return
  erroForm.value = ''
  sucesso.value = ''
  if (!disciplinaId.value || !['PROVA', 'ENTREGA'].includes(tipo.value) || !data.value) {
    erroForm.value = 'Selecione a disciplina, o tipo e a data.'
    return
  }
  if (!descricao.value.trim() || descricao.value.trim().length > 500) {
    erroForm.value = 'Informe uma descrição de até 500 caracteres.'
    return
  }
  salvando.value = true
  try {
    const criada = await api.criar({ disciplinaId: disciplinaId.value, tipo: tipo.value, descricao: descricao.value.trim(), data: data.value })
    avaliacoes.value = [...avaliacoes.value, criada]
    descricao.value = ''
    data.value = ''
    sucesso.value = 'Avaliação cadastrada.'
  } catch (erro) {
    erroForm.value = mensagemDeErro(erro, 'Não foi possível cadastrar a avaliação.')
  } finally {
    salvando.value = false
  }
}

watch(disciplinaId, () => {
  sucesso.value = ''
  erroForm.value = ''
  carregarAvaliacoes()
})
onMounted(carregarDisciplinas)
</script>

<template>
  <div class="colaboracao">
    <header><h1>Provas e entregas</h1><p>Organize suas avaliações e acompanhe as próximas datas.</p></header>
    <p v-if="sucesso" class="sucesso" role="status">{{ sucesso }}</p>
    <section class="painel">
      <h2>Nova avaliação</h2>
      <p v-if="carregandoDisciplinas" role="status">Carregando disciplinas…</p>
      <div v-else-if="erroDisciplinas" class="erro" role="alert">
        <p>{{ erroDisciplinas }}</p><button @click="carregarDisciplinas">Tentar novamente</button>
      </div>
      <p v-else-if="!disciplinas.length" class="muted">Você precisa <NuxtLink class="link" to="/disciplinas">criar uma disciplina</NuxtLink> para cadastrar avaliações.</p>
      <form v-else @submit.prevent="cadastrar">
        <label>Disciplina
          <select aria-label="Disciplina" v-model="disciplinaId" required :disabled="salvando">
            <option v-for="disciplina in disciplinas" :key="disciplina.id" :value="disciplina.id">{{ disciplina.nome }}</option>
          </select>
        </label>
        <div class="campos">
          <label>Tipo<select aria-label="Tipo" v-model="tipo" required :disabled="salvando"><option value="PROVA">Prova</option><option value="ENTREGA">Entrega</option></select></label>
          <label>Data<input v-model="data" type="date" required :disabled="salvando" /></label>
        </div>
        <label>Descrição<textarea v-model="descricao" rows="3" maxlength="500" required :disabled="salvando" placeholder="Ex.: Prova dos capítulos 1 e 2" /></label>
        <p v-if="erroForm" class="erro" role="alert">{{ erroForm }}</p>
        <div class="acoes"><button type="submit" class="primario" :disabled="salvando || carregando">{{ salvando ? 'Salvando…' : 'Cadastrar avaliação' }}</button></div>
      </form>
    </section>
    <section v-if="disciplinaId" class="painel" aria-live="polite">
      <h2>Próximas avaliações</h2>
      <p v-if="carregando" role="status">Carregando avaliações…</p>
      <div v-else-if="erroLista" class="erro" role="alert"><p>{{ erroLista }}</p><button @click="carregarAvaliacoes">Tentar novamente</button></div>
      <template v-else>
        <p v-if="!proximas.length" class="muted">Nenhuma avaliação futura nesta disciplina.</p>
        <ul v-else class="lista">
          <li v-for="item in proximas" :key="item.id" class="item">
            <div><span class="tipo" :class="{ entrega: item.tipo === 'ENTREGA' }">{{ item.tipo === 'PROVA' ? 'Prova' : 'Entrega' }}</span><p>{{ item.descricao }}</p></div>
            <time :datetime="item.data">{{ formatarData(item.data) }}</time>
          </li>
        </ul>
        <details v-if="anteriores.length" class="mt-6">
          <summary class="cursor-pointer font-semibold">Avaliações anteriores ({{ anteriores.length }})</summary>
          <ul class="lista"><li v-for="item in anteriores" :key="item.id" class="item"><p>{{ item.tipo === 'PROVA' ? 'Prova' : 'Entrega' }} · {{ item.descricao }}</p><time :datetime="item.data">{{ formatarData(item.data) }}</time></li></ul>
        </details>
      </template>
    </section>
  </div>
</template>

<style src="~/assets/css/colaboracao.css" />
