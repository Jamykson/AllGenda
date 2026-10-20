<script setup lang="ts">
import { ArrowLeft } from '@lucide/vue'
import type { RouteLocationNormalized } from 'vue-router'
import type { Anotacao, AulaDaPagina, ImagemAula, ModoPainel } from '~/types/anotacao'

// Página de uma aula: galeria de imagens avulsas no topo (painel ajustável), lista de anotações à
// esquerda e editor da anotação selecionada no centro.
//
// Uma aula recorrente ainda não existe no backend e chega como /aulas/recorrente?disciplinaId=&data=&horario=.
// Ela é cadastrada quando recebe a primeira anotação ou imagem, e então a URL passa a ser /aulas/{id}.
// A chave fixa mantém a página montada nessa troca, para o editor não perder o que está sendo digitado.
definePageMeta({ key: 'pagina-da-aula' })

interface EditorExposto {
  salvarAgora: () => Promise<void>
  temPendencia: () => boolean
  prepararSaida: () => Promise<boolean>
}

const FRACAO_INICIAL = 0.4
const FRACAO_MINIMA = 0.15
const FRACAO_MAXIMA = 0.85

const route = useRoute()
const router = useRouter()
const { buscarAula, cadastrarAulaRecorrente, listarPorAula, listarImagensDaAula, listarTags } = useAnotacoes()
const { listar: listarDisciplinas } = useDisciplinas()

const aula = ref<AulaDaPagina | null>(null)
const carregandoAula = ref(true)
const erroAula = ref('')
const anotacoes = ref<Anotacao[]>([])
const imagens = ref<ImagemAula[]>([])
const carregandoConteudo = ref(false)
const erroConteudo = ref('')
const tagsDisponiveis = ref<string[]>([])

// null: o editor está com uma anotação nova; a chave só muda quando a pessoa troca de anotação,
// para que a anotação nova, ao ser salva, continue no mesmo editor
const selecionadaId = ref<string | null>(null)
const chaveEditor = ref(0)
const editor = ref<EditorExposto | null>(null)

const modoPainel = ref<ModoPainel>('normal')
const fracaoPainel = ref(FRACAO_INICIAL)
const areaAula = ref<HTMLElement | null>(null)
let redimensionando = false

let chaveCarregada = ''
let cadastroDaAula: Promise<string> | null = null

const anotacaoSelecionada = computed(() =>
  anotacoes.value.find(anotacao => anotacao.id === selecionadaId.value) ?? null
)
const aulaIdNoBackend = computed(() => (aula.value && !aula.value.recorrente ? aula.value.id : null))
const estiloPainel = computed(() => {
  if (modoPainel.value === 'normal') return { height: `${fracaoPainel.value * 100}%` }
  if (modoPainel.value === 'maximizado') return { flex: '1 1 auto' }
  return {}
})

function texto(valor: unknown) {
  return typeof valor === 'string' ? valor : ''
}

// identifica a aula de uma rota; a aula recorrente é identificada pela disciplina, data e horário
function chaveDaRota(rota: RouteLocationNormalized) {
  const id = texto(rota.params.id)
  if (id !== 'recorrente') return id
  return ['recorrente', rota.query.disciplinaId, rota.query.data, rota.query.horario].map(texto).join('|')
}

async function carregar() {
  const chave = chaveDaRota(route)
  chaveCarregada = chave
  aula.value = null
  erroAula.value = ''
  carregandoAula.value = true
  anotacoes.value = []
  imagens.value = []
  selecionadaId.value = null

  try {
    // a lista de disciplinas traz o nome personalizado; sem ela, fica o nome do backend
    const disciplinas = listarDisciplinas().catch(() => [])

    if (texto(route.params.id) === 'recorrente') {
      const disciplinaId = texto(route.query.disciplinaId)
      const data = texto(route.query.data)
      const horario = texto(route.query.horario)
      if (!disciplinaId || !data || !horario) {
        erroAula.value = 'O endereço desta aula está incompleto.'
        return
      }
      const disciplina = (await disciplinas).find(item => item.id === disciplinaId)
      if (!disciplina) {
        erroAula.value = 'Não foi possível encontrar a disciplina desta aula.'
        return
      }
      aula.value = {
        id: `recorrente-${disciplinaId}-${data}`,
        data,
        horario,
        topico: texto(route.query.topico) || null,
        recorrente: true,
        disciplinaId,
        disciplinaNome: disciplina.nome
      }
    }
    else {
      const [encontrada, lista] = await Promise.all([buscarAula(texto(route.params.id)), disciplinas])
      const disciplina = lista.find(item => item.id === encontrada.disciplinaId)
      aula.value = { ...encontrada, disciplinaNome: disciplina?.nome ?? encontrada.disciplinaNome }
    }
  }
  catch (erro) {
    console.error('[aula] erro ao carregar a aula:', erro)
    erroAula.value = mensagemDeErro(erro, 'Não foi possível carregar a aula.')
    return
  }
  finally {
    if (chave === chaveCarregada) carregandoAula.value = false
  }

  if (chave === chaveCarregada) await carregarConteudo(chave)
}

async function carregarConteudo(chave: string) {
  erroConteudo.value = ''
  if (aula.value && !aula.value.recorrente) {
    carregandoConteudo.value = true
    try {
      const [anotacoesDaAula, imagensDaAula] = await Promise.all([
        listarPorAula(aula.value.id),
        listarImagensDaAula(aula.value.id)
      ])
      if (chave !== chaveCarregada) return
      anotacoes.value = anotacoesDaAula
      imagens.value = imagensDaAula
    }
    catch (erro) {
      console.error('[aula] erro ao carregar o conteúdo da aula:', erro)
      erroConteudo.value = mensagemDeErro(erro, 'Não foi possível carregar as anotações e imagens desta aula.')
    }
    finally {
      carregandoConteudo.value = false
    }
  }

  // abre a primeira anotação; sem anotações, o editor já começa uma nova
  selecionadaId.value = anotacoes.value[0]?.id ?? null
  chaveEditor.value++
}

async function carregarTags() {
  try {
    tagsDisponiveis.value = await listarTags()
  }
  catch (erro) {
    // sem a lista, ainda dá para digitar tags novas
    console.error('[aula] erro ao carregar tags:', erro)
  }
}

// garante que a aula exista no backend; anotações e fotos enviadas juntas esperam o mesmo cadastro
function obterAulaId(): Promise<string> {
  const atual = aula.value
  if (!atual) return Promise.reject(new Error('A aula ainda não foi carregada.'))
  if (!atual.recorrente) return Promise.resolve(atual.id)

  cadastroDaAula ??= (async () => {
    const cadastrada = await cadastrarAulaRecorrente(atual)
    aula.value = { ...atual, ...cadastrada, recorrente: false, disciplinaNome: atual.disciplinaNome }
    chaveCarregada = cadastrada.id // a troca de URL a seguir não recarrega a página
    await router.replace(`/aulas/${cadastrada.id}`)
    return cadastrada.id
  })().finally(() => {
    cadastroDaAula = null
  })
  return cadastroDaAula
}

async function selecionar(anotacaoId: string | null) {
  if (anotacaoId !== null && anotacaoId === selecionadaId.value) return
  if (editor.value && !(await editor.value.prepararSaida())) return
  selecionadaId.value = anotacaoId
  chaveEditor.value++
}

function aoSalvarAnotacao(anotacao: Anotacao) {
  const indice = anotacoes.value.findIndex(item => item.id === anotacao.id)
  if (indice >= 0) {
    anotacoes.value = anotacoes.value.map(item => (item.id === anotacao.id ? anotacao : item))
    return
  }
  // a anotação nova acabou de ser criada: entra no fim da lista e continua no editor
  anotacoes.value = [...anotacoes.value, anotacao]
  if (selecionadaId.value === null) selecionadaId.value = anotacao.id
  for (const tag of anotacao.tags) {
    if (!tagsDisponiveis.value.some(item => item.toLowerCase() === tag.toLowerCase())) {
      tagsDisponiveis.value = [...tagsDisponiveis.value, tag]
    }
  }
}

function aoExcluirAnotacao(anotacaoId: string) {
  const indice = anotacoes.value.findIndex(item => item.id === anotacaoId)
  anotacoes.value = anotacoes.value.filter(item => item.id !== anotacaoId)
  const vizinha = anotacoes.value[Math.min(indice, anotacoes.value.length - 1)]
  selecionadaId.value = vizinha?.id ?? null
  chaveEditor.value++
}

function voltar() {
  // sem histórico (link aberto direto), volta para a agenda
  if (window.history.state?.back) router.back()
  else navigateTo('/agenda')
}

function definirFracao(fracao: number) {
  fracaoPainel.value = Math.min(FRACAO_MAXIMA, Math.max(FRACAO_MINIMA, fracao))
}

function iniciarRedimensionamento(evento: PointerEvent) {
  redimensionando = true
  ;(evento.currentTarget as HTMLElement).setPointerCapture(evento.pointerId)
}

function redimensionar(evento: PointerEvent) {
  if (!redimensionando || !areaAula.value) return
  const caixa = areaAula.value.getBoundingClientRect()
  definirFracao((evento.clientY - caixa.top) / caixa.height)
}

function terminarRedimensionamento() {
  redimensionando = false
}

function ajustarComTeclado(evento: KeyboardEvent) {
  const passo = 0.05
  if (evento.key === 'ArrowUp') definirFracao(fracaoPainel.value - passo)
  else if (evento.key === 'ArrowDown') definirFracao(fracaoPainel.value + passo)
  else if (evento.key === 'Home') definirFracao(FRACAO_MINIMA)
  else if (evento.key === 'End') definirFracao(FRACAO_MAXIMA)
  else return
  evento.preventDefault()
}

// fechar ou recarregar a aba com texto ainda não salvo pede confirmação ao navegador
function avisarAoFecharAba(evento: BeforeUnloadEvent) {
  if (editor.value?.temPendencia()) {
    evento.preventDefault()
    evento.returnValue = ''
  }
}

watch(() => route.fullPath, () => {
  if (chaveDaRota(route) !== chaveCarregada) void carregar()
})

// antes de ir para outra aula ou sair da página, o editor salva o que estiver pendente
onBeforeRouteUpdate(async (destino) => {
  if (chaveDaRota(destino) === chaveCarregada) return true
  return editor.value ? await editor.value.prepararSaida() : true
})

onBeforeRouteLeave(async () => (editor.value ? await editor.value.prepararSaida() : true))

onMounted(() => {
  window.addEventListener('beforeunload', avisarAoFecharAba)
  void carregar()
  void carregarTags()
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', avisarAoFecharAba)
})

useHead(() => ({ title: aula.value ? `${aula.value.disciplinaNome} · ${formatarDataDaAula(aula.value.data)}` : 'Aula' }))
</script>

<template>
  <div class="pagina-aula">
    <header class="cabecalho-aula">
      <button type="button" class="voltar" aria-label="Voltar" title="Voltar" @click="voltar">
        <ArrowLeft :size="18" :stroke-width="1.8" aria-hidden="true" />
      </button>
      <div class="identificacao">
        <h1>{{ aula?.disciplinaNome ?? (carregandoAula ? 'Carregando aula...' : 'Aula') }}</h1>
        <p v-if="aula">
          <span v-if="aula.topico">{{ aula.topico }} · </span>
          {{ formatarDataDaAula(aula.data) }} · {{ aula.horario }}
          <span v-if="aula.recorrente" class="selo">Aula recorrente</span>
        </p>
      </div>
    </header>

    <p v-if="erroAula" class="mensagem-erro erro-pagina" role="alert">{{ erroAula }}</p>

    <div v-else-if="aula" ref="areaAula" class="area-aula">
      <div class="painel-imagens" :class="`painel-${modoPainel}`" :style="estiloPainel">
        <PainelGaleria
          v-model:imagens="imagens"
          v-model:modo="modoPainel"
          :aula-id="aulaIdNoBackend"
          :obter-aula-id="obterAulaId"
          :carregando="carregandoConteudo"
        />
      </div>

      <div
        v-if="modoPainel === 'normal'"
        class="alca"
        role="separator"
        tabindex="0"
        aria-orientation="horizontal"
        aria-label="Ajustar altura da galeria"
        :aria-valuenow="Math.round(fracaoPainel * 100)"
        :aria-valuemin="Math.round(FRACAO_MINIMA * 100)"
        :aria-valuemax="Math.round(FRACAO_MAXIMA * 100)"
        title="Arraste para ajustar a altura da galeria (duplo clique volta ao tamanho inicial)"
        @pointerdown="iniciarRedimensionamento"
        @pointermove="redimensionar"
        @pointerup="terminarRedimensionamento"
        @pointercancel="terminarRedimensionamento"
        @dblclick="fracaoPainel = FRACAO_INICIAL"
        @keydown="ajustarComTeclado"
      >
        <span class="pegador" aria-hidden="true" />
      </div>

      <div v-show="modoPainel !== 'maximizado'" class="area-anotacoes">
        <ListaAnotacoes
          :anotacoes="anotacoes"
          :selecionada-id="selecionadaId"
          :carregando="carregandoConteudo"
          @selecionar="selecionar"
          @nova="selecionar(null)"
        />
        <div class="area-editor">
          <p v-if="erroConteudo" class="mensagem-erro erro-conteudo" role="alert">{{ erroConteudo }}</p>
          <EditorAnotacao
            v-else-if="!carregandoConteudo"
            :key="chaveEditor"
            ref="editor"
            :anotacao="anotacaoSelecionada"
            :tags-disponiveis="tagsDisponiveis"
            :obter-aula-id="obterAulaId"
            @salva="aoSalvarAnotacao"
            @excluida="aoExcluirAnotacao"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.pagina-aula {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100vh;
  min-height: 0;
  overflow: hidden;
  background: #f7f8fa;
}

.cabecalho-aula {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid #e2e8f0;
  background: #fff;
}

.voltar {
  display: grid;
  flex: 0 0 36px;
  width: 36px;
  height: 36px;
  place-items: center;
  padding: 0;
  border: 1px solid #dfe3e8;
  border-radius: 8px;
  background: #fff;
  color: #334155;
  cursor: pointer;
}

.voltar:hover {
  background: #f1f5f9;
}

.identificacao {
  min-width: 0;
}

h1 {
  overflow: hidden;
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.identificacao p {
  margin: 2px 0 0;
  color: #64748b;
  font-size: 13px;
}

.selo {
  margin-left: 6px;
  padding: 1px 8px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #475569;
  font-size: 11px;
  font-weight: 600;
}

.area-aula {
  position: relative;
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.painel-imagens {
  flex: 0 0 auto;
  min-height: 0;
  overflow: hidden;
}

.painel-recolhido {
  border-bottom: 1px solid #e2e8f0;
}

.alca {
  position: relative;
  flex: 0 0 10px;
  display: grid;
  place-items: center;
  border-top: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
  background: #f1f5f9;
  cursor: row-resize;
  touch-action: none;
}

.alca:hover,
.alca:focus-visible {
  background: #dbeafe;
  outline: none;
}

.pegador {
  width: 40px;
  height: 4px;
  border-radius: 999px;
  background: #94a3b8;
}

.area-anotacoes {
  display: grid;
  flex: 1;
  grid-template-columns: 300px minmax(0, 1fr);
  min-height: 0;
  background: #fff;
}

.area-editor {
  min-width: 0;
  min-height: 0;
}

.mensagem-erro {
  margin: 0;
  color: #b91c1c;
  font-size: 14px;
}

.erro-pagina,
.erro-conteudo {
  padding: 20px 24px;
}

.voltar:focus-visible {
  outline: 2px solid rgb(37 99 235 / 55%);
  outline-offset: 2px;
}

@media (max-width: 900px) {
  .area-anotacoes {
    grid-template-columns: minmax(0, 1fr);
    grid-template-rows: minmax(0, 35%) minmax(0, 1fr);
  }
}
</style>
