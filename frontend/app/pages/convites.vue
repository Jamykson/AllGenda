<script setup lang="ts">
import type { Convite } from '~/types/convite'
import type { Disciplina } from '~/types/disciplina'
import { mensagemDeErro } from '~/composables/useAnotacoes'

const api = useConvites()
const disciplinasApi = useDisciplinas()
const disciplinas = ref<Disciplina[]>([])
const disciplinaId = ref('')
const email = ref('')
const convites = ref<Convite[]>([])
const carregandoDisciplinas = ref(true)
const carregandoConvites = ref(true)
const enviando = ref(false)
const respondendo = ref<string[]>([])
const erroDisciplinas = ref('')
const erroEnvio = ref('')
const erroLista = ref('')
const errosResposta = reactive<Record<string, string>>({})
const sucessoEnvio = ref('')
const sucessoResposta = ref('')
const disciplinaAceita = ref('')

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

async function carregarConvites() {
  carregandoConvites.value = true
  erroLista.value = ''
  try {
    convites.value = await api.listarPendentes()
  } catch (erro) {
    erroLista.value = mensagemDeErro(erro, 'Não foi possível carregar os convites.')
  } finally {
    carregandoConvites.value = false
  }
}

async function enviar() {
  if (enviando.value) return
  erroEnvio.value = ''
  sucessoEnvio.value = ''
  if (!disciplinaId.value || email.value.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim())) {
    erroEnvio.value = 'Selecione uma disciplina e informe um e-mail válido.'
    return
  }
  enviando.value = true
  try {
    await api.enviar(disciplinaId.value, email.value.trim())
    sucessoEnvio.value = `Convite enviado para ${email.value.trim()}.`
    email.value = ''
  } catch (erro) {
    erroEnvio.value = mensagemDeErro(erro, 'Não foi possível enviar o convite.')
  } finally {
    enviando.value = false
  }
}

async function responder(convite: Convite, resposta: 'aceitar' | 'recusar') {
  if (respondendo.value.includes(convite.id)) return
  respondendo.value.push(convite.id)
  delete errosResposta[convite.id]
  sucessoResposta.value = ''
  disciplinaAceita.value = ''
  try {
    await api.responder(convite.id, resposta)
    convites.value = convites.value.filter(item => item.id !== convite.id)
    sucessoResposta.value = resposta === 'aceitar'
      ? `Você agora participa de ${convite.disciplinaNome}.`
      : `Convite para ${convite.disciplinaNome} recusado.`
    if (resposta === 'aceitar') disciplinaAceita.value = convite.disciplinaId
  } catch (erro) {
    errosResposta[convite.id] = mensagemDeErro(erro, 'Não foi possível responder ao convite. Atualize a lista e tente novamente.')
  } finally {
    respondendo.value = respondendo.value.filter(id => id !== convite.id)
  }
}

onMounted(async () => {
  await Promise.allSettled([carregarDisciplinas(), carregarConvites()])
})
</script>

<template>
  <div class="colaboracao">
    <header><h1>Convites</h1><p>Compartilhe uma disciplina com colegas e responda aos seus convites.</p></header>
    <section class="painel">
      <h2>Convidar um colega</h2>
      <p class="muted mb-4">Apenas o dono da disciplina pode convidar. Seu colega precisa ter uma conta com o e-mail informado.</p>
      <p v-if="carregandoDisciplinas" role="status">Carregando disciplinas…</p>
      <div v-else-if="erroDisciplinas" class="erro" role="alert"><p>{{ erroDisciplinas }}</p><button @click="carregarDisciplinas">Tentar novamente</button></div>
      <p v-else-if="!disciplinas.length" class="muted">Você precisa <NuxtLink class="link" to="/disciplinas">criar uma disciplina</NuxtLink> para convidar colegas.</p>
      <form v-else @submit.prevent="enviar">
        <label>Disciplina<select aria-label="Disciplina" v-model="disciplinaId" required :disabled="enviando"><option v-for="disciplina in disciplinas" :key="disciplina.id" :value="disciplina.id">{{ disciplina.nome }}</option></select></label>
        <label>E-mail do colega<input v-model="email" type="email" autocomplete="email" required maxlength="254" placeholder="colega@example.com" :disabled="enviando" /></label>
        <p v-if="erroEnvio" class="erro" role="alert">{{ erroEnvio }}</p>
        <p v-if="sucessoEnvio" class="sucesso" role="status">{{ sucessoEnvio }}</p>
        <div class="acoes"><button type="submit" class="primario" :disabled="enviando">{{ enviando ? 'Enviando…' : 'Enviar convite' }}</button></div>
      </form>
    </section>
    <section class="painel">
      <div class="acoes mb-4"><h2 class="!mb-0">Convites pendentes</h2><button :disabled="carregandoConvites || respondendo.length > 0" @click="carregarConvites">Atualizar</button></div>
      <div v-if="sucessoResposta" class="sucesso" role="status">
        <p>{{ sucessoResposta }}</p>
        <NuxtLink v-if="disciplinaAceita" :to="`/disciplinas/${disciplinaAceita}`" class="link mt-2">Abrir disciplina</NuxtLink>
      </div>
      <p v-if="carregandoConvites" role="status">Carregando convites…</p>
      <p v-else-if="erroLista" class="erro" role="alert">{{ erroLista }}</p>
      <p v-else-if="!convites.length" class="muted">Você não tem convites pendentes.</p>
      <ul v-else class="lista">
        <li v-for="convite in convites" :key="convite.id" class="item">
          <div><strong>{{ convite.disciplinaNome }}</strong><p class="muted">Convite para participar como membro.</p><p v-if="errosResposta[convite.id]" class="erro" role="alert">{{ errosResposta[convite.id] }}</p></div>
          <div class="acoes">
            <button class="primario" :disabled="respondendo.includes(convite.id)" :aria-label="`Aceitar convite para ${convite.disciplinaNome}`" @click="responder(convite, 'aceitar')">{{ respondendo.includes(convite.id) ? 'Enviando…' : 'Aceitar' }}</button>
            <button :disabled="respondendo.includes(convite.id)" :aria-label="`Recusar convite para ${convite.disciplinaNome}`" @click="responder(convite, 'recusar')">Recusar</button>
          </div>
        </li>
      </ul>
    </section>
  </div>
</template>

<style src="~/assets/css/colaboracao.css" />
