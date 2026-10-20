<script setup lang="ts">
import MarkdownIt from 'markdown-it'
import { ArrowUp, PanelRightClose, PanelRightOpen, RotateCcw, Sparkles, Square, SquarePen, X } from '@lucide/vue'
import type { FocoAssistente, MensagemAssistente } from '~/types/assistente'

// Barra lateral direita da página da aula: um chat com o assistente de IA, que responde usando as
// anotações e as imagens da aula. A conversa fica só aqui (o servidor não a guarda) e recomeça ao
// trocar de aula. Recolhida, vira uma faixa estreita; em telas estreitas, uma gaveta sobre a página.

const props = defineProps<{
  // null enquanto a aula não existe no backend (aula recorrente ainda sem anotações nem imagens)
  aulaId: string | null
  recolhida: boolean
  // a anotação ou imagem sobre a qual será a próxima pergunta
  foco: FocoAssistente | null
}>()

const emit = defineEmits<{
  alternar: []
  'update:foco': [foco: FocoAssistente | null]
}>()

const SUGESTOES = [
  { rotulo: 'Resumir a aula', pergunta: 'Faça um resumo da aula.' },
  { rotulo: 'Conceitos principais', pergunta: 'Liste os conceitos principais da aula, com uma explicação curta de cada um.' },
  { rotulo: 'Perguntas de revisão', pergunta: 'Crie 5 perguntas de revisão sobre a aula, com as respostas no final.' }
]
// o back-end recusa perguntas maiores
const TAMANHO_MAXIMO_PERGUNTA = 4000

// html: false escapa qualquer HTML da resposta, e o markdown-it já recusa links javascript:
const markdown = new MarkdownIt({ html: false, linkify: true, breaks: true })
const abrirLink = markdown.renderer.rules.link_open
markdown.renderer.rules.link_open = (tokens, indice, opcoes, ambiente, renderizador) => {
  tokens[indice]!.attrSet('target', '_blank')
  tokens[indice]!.attrSet('rel', 'noopener noreferrer')
  return abrirLink
    ? abrirLink(tokens, indice, opcoes, ambiente, renderizador)
    : renderizador.renderToken(tokens, indice, opcoes)
}

const { perguntar } = useAssistente()

const mensagens = ref<MensagemAssistente[]>([])
const rascunho = ref('')
const respondendo = ref(false)
// pergunta que falhou, para tentar de novo; ela sai da conversa, que precisa alternar pergunta e resposta
const falha = ref<{ mensagem: string, pergunta: string, foco: FocoAssistente | null } | null>(null)
const areaMensagens = ref<HTMLElement | null>(null)
const campo = ref<HTMLTextAreaElement | null>(null)
let controlador: AbortController | null = null
// muda a cada nova conversa; uma resposta de uma conversa já descartada não mexe na atual
let conversaAtual = 0

const podePerguntar = computed(() => Boolean(props.aulaId) && !respondendo.value)

function renderizar(texto: string) {
  return markdown.render(texto)
}

// acompanha a resposta enquanto ela chega, a menos que o aluno tenha rolado para ler algo acima
function rolarParaOFim(forcar = false) {
  const area = areaMensagens.value
  if (!area) return
  const pertoDoFim = area.scrollHeight - area.scrollTop - area.clientHeight < 80
  if (forcar || pertoDoFim) {
    void nextTick(() => {
      area.scrollTop = area.scrollHeight
    })
  }
}

async function enviar(texto = rascunho.value, foco = props.foco) {
  const pergunta = texto.trim()
  if (!pergunta || !podePerguntar.value || !props.aulaId) return

  const historico: MensagemAssistente[] = [...mensagens.value, { papel: 'USUARIO', texto: pergunta, foco }]
  mensagens.value = [...historico, { papel: 'ASSISTENTE', texto: '' }]
  const indiceResposta = mensagens.value.length - 1
  const conversa = conversaAtual
  rascunho.value = ''
  falha.value = null
  emit('update:foco', null)
  respondendo.value = true
  controlador = new AbortController()
  rolarParaOFim(true)

  try {
    await perguntar(props.aulaId, historico, (pedaco) => {
      if (conversa !== conversaAtual) return
      mensagens.value[indiceResposta]!.texto += pedaco
      rolarParaOFim()
    }, controlador.signal)
  }
  catch (erro) {
    if (conversa !== conversaAtual) return
    const resposta = mensagens.value[indiceResposta]
    if (controlador?.signal.aborted) {
      // parada pelo aluno: o que já chegou fica, marcado; sem nada, a pergunta volta para o campo
      if (resposta?.texto.trim()) {
        resposta.interrompida = true
      }
      else {
        mensagens.value = mensagens.value.slice(0, indiceResposta - 1)
        rascunho.value = pergunta
        emit('update:foco', foco)
      }
    }
    else {
      console.error('[assistente] erro ao perguntar:', erro)
      mensagens.value = mensagens.value.slice(0, indiceResposta - 1)
      falha.value = {
        mensagem: erro instanceof Error && erro.message ? erro.message : 'Não foi possível falar com o assistente.',
        pergunta,
        foco
      }
    }
  }
  finally {
    if (conversa === conversaAtual) {
      respondendo.value = false
      controlador = null
      rolarParaOFim()
    }
  }
}

function parar() {
  controlador?.abort()
}

function novaConversa() {
  parar()
  conversaAtual++
  respondendo.value = false
  controlador = null
  mensagens.value = []
  falha.value = null
  rascunho.value = ''
  emit('update:foco', null)
  campo.value?.focus()
}

function aoPressionarTecla(evento: KeyboardEvent) {
  // Enter envia; Shift+Enter quebra a linha; durante a composição (acentos em alguns teclados), nada
  if (evento.key === 'Enter' && !evento.shiftKey && !evento.isComposing) {
    evento.preventDefault()
    void enviar()
  }
}

// o campo cresce com o texto até um limite, e a partir daí rola
function ajustarAltura() {
  const elemento = campo.value
  if (!elemento) return
  elemento.style.height = 'auto'
  elemento.style.height = `${Math.min(elemento.scrollHeight, 160)}px`
}

watch(rascunho, () => nextTick(ajustarAltura))

// ao trocar de aula, a conversa recomeça
watch(() => props.aulaId, (atual, anterior) => {
  if (anterior && atual !== anterior) novaConversa()
})

// "Perguntar ao assistente" numa imagem ou anotação abre o painel com o foco: o campo já fica pronto
watch(() => props.foco, async (foco) => {
  if (!foco || props.recolhida) return
  await nextTick()
  campo.value?.focus()
})

watch(() => props.recolhida, async (recolhida) => {
  if (recolhida) return
  await nextTick()
  campo.value?.focus()
  rolarParaOFim(true)
})

onBeforeUnmount(parar)
</script>

<template>
  <aside v-if="recolhida" class="assistente recolhido" aria-label="Assistente da aula">
    <button
      type="button"
      class="botao-icone"
      aria-label="Abrir assistente"
      title="Abrir assistente"
      aria-expanded="false"
      @click="emit('alternar')"
    >
      <PanelRightOpen class="icone-trilho" :size="18" :stroke-width="1.8" aria-hidden="true" />
      <Sparkles class="icone-flutuante" :size="22" :stroke-width="1.8" aria-hidden="true" />
    </button>
    <Sparkles class="marca" :size="18" :stroke-width="1.8" aria-hidden="true" />
  </aside>

  <aside v-else class="assistente" aria-label="Assistente da aula">
    <header class="topo">
      <h2>
        <Sparkles :size="15" :stroke-width="2" aria-hidden="true" />
        Assistente
      </h2>
      <div class="acoes-topo">
        <button
          type="button"
          class="botao-icone"
          aria-label="Nova conversa"
          title="Nova conversa"
          :disabled="!mensagens.length && !falha"
          @click="novaConversa"
        >
          <SquarePen :size="17" :stroke-width="1.8" aria-hidden="true" />
        </button>
        <button
          type="button"
          class="botao-icone"
          aria-label="Recolher assistente"
          title="Recolher assistente"
          aria-expanded="true"
          @click="emit('alternar')"
        >
          <PanelRightClose :size="18" :stroke-width="1.8" aria-hidden="true" />
        </button>
      </div>
    </header>

    <div ref="areaMensagens" class="mensagens" aria-live="polite">
      <div v-if="!aulaId" class="vazio">
        <p>Quando a aula tiver anotações ou imagens, você poderá perguntar sobre elas.</p>
      </div>

      <div v-else-if="!mensagens.length && !falha" class="vazio">
        <p>Pergunte sobre as anotações e as imagens desta aula.</p>
        <div class="sugestoes">
          <button
            v-for="sugestao in SUGESTOES"
            :key="sugestao.rotulo"
            type="button"
            class="sugestao"
            @click="enviar(sugestao.pergunta, null)"
          >
            {{ sugestao.rotulo }}
          </button>
        </div>
      </div>

      <template v-for="(mensagem, indice) in mensagens" :key="indice">
        <div v-if="mensagem.papel === 'USUARIO'" class="mensagem-usuario">
          <span v-if="mensagem.foco" class="foco-da-mensagem">Sobre: {{ mensagem.foco.rotulo }}</span>
          <p>{{ mensagem.texto }}</p>
        </div>
        <div v-else class="mensagem-assistente">
          <p v-if="!mensagem.texto && respondendo" class="pensando" role="status">Pensando...</p>
          <!-- eslint-disable-next-line vue/no-v-html -- markdown-it com html: false escapa o HTML da resposta -->
          <div v-else class="texto-resposta" v-html="renderizar(mensagem.texto)" />
          <p v-if="mensagem.interrompida" class="nota-resposta">Resposta interrompida.</p>
        </div>
      </template>

      <div v-if="falha" class="falha" role="alert">
        <p class="pergunta-falha">{{ falha.pergunta }}</p>
        <p class="mensagem-erro">{{ falha.mensagem }}</p>
        <button type="button" class="tentar-de-novo" @click="enviar(falha.pergunta, falha.foco)">
          <RotateCcw :size="14" :stroke-width="2" aria-hidden="true" />
          Tentar de novo
        </button>
      </div>
    </div>

    <form class="composicao" @submit.prevent="enviar()">
      <span v-if="foco" class="chip-foco">
        <span class="rotulo-foco">Sobre: {{ foco.rotulo }}</span>
        <button type="button" aria-label="Tirar o foco" title="Tirar o foco" @click="emit('update:foco', null)">
          <X :size="13" :stroke-width="2" aria-hidden="true" />
        </button>
      </span>
      <div class="campo-e-botao">
        <textarea
          ref="campo"
          v-model="rascunho"
          rows="1"
          aria-label="Pergunta ao assistente"
          :placeholder="aulaId ? 'Pergunte sobre a aula...' : 'Sem material para perguntar ainda'"
          :maxlength="TAMANHO_MAXIMO_PERGUNTA"
          :disabled="!aulaId"
          @keydown="aoPressionarTecla"
        />
        <button
          v-if="respondendo"
          type="button"
          class="botao-enviar"
          aria-label="Parar a resposta"
          title="Parar"
          @click="parar"
        >
          <Square :size="13" :stroke-width="2.5" fill="currentColor" aria-hidden="true" />
        </button>
        <button
          v-else
          type="submit"
          class="botao-enviar"
          aria-label="Enviar pergunta"
          title="Enviar (Enter)"
          :disabled="!aulaId || !rascunho.trim()"
        >
          <ArrowUp :size="17" :stroke-width="2.2" aria-hidden="true" />
        </button>
      </div>
      <p class="aviso">As anotações e imagens desta aula são enviadas à Anthropic para gerar as respostas.</p>
    </form>
  </aside>
</template>

<style scoped>
.assistente {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  border-left: 1px solid #e2e8f0;
  background: #f8fafc;
}

.recolhido {
  align-items: center;
  gap: 10px;
  padding: 12px 0;
}

.marca {
  color: #7c3aed;
}

.icone-flutuante {
  display: none;
}

.topo {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 57px;
  padding: 12px 14px;
  border-bottom: 1px solid #e2e8f0;
}

h2 {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

h2 svg {
  color: #7c3aed;
}

.acoes-topo {
  display: flex;
  gap: 2px;
}

.botao-icone {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 7px;
  background: transparent;
  color: #475569;
  cursor: pointer;
}

.botao-icone:hover:not(:disabled) {
  border-color: #e2e8f0;
  background: #fff;
  color: #1d4ed8;
}

.botao-icone:disabled {
  opacity: 0.4;
  cursor: default;
}

.mensagens {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
  padding: 16px 14px;
  overflow-y: auto;
}

.vazio {
  display: grid;
  gap: 12px;
  margin: auto 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
  text-align: center;
}

.vazio p {
  margin: 0;
}

.sugestoes {
  display: grid;
  gap: 8px;
}

.sugestao {
  padding: 9px 12px;
  border: 1px solid #ddd6fe;
  border-radius: 9px;
  background: #fff;
  color: #5b21b6;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
}

.sugestao:hover {
  border-color: #a78bfa;
  background: #f5f3ff;
}

.mensagem-usuario {
  display: grid;
  gap: 4px;
  align-self: flex-end;
  max-width: 88%;
  padding: 9px 12px;
  border-radius: 12px 12px 3px 12px;
  background: #2563eb;
  color: #fff;
  font-size: 14px;
  line-height: 1.45;
}

.mensagem-usuario p {
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.foco-da-mensagem {
  color: #dbeafe;
  font-size: 11px;
  font-weight: 600;
}

.mensagem-assistente {
  color: #0f172a;
  font-size: 14px;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.pensando {
  margin: 0;
  color: #64748b;
  font-style: italic;
}

.nota-resposta {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 12px;
  font-style: italic;
}

/* o HTML da resposta vem do markdown-it, fora do escopo deste componente */
.texto-resposta :deep(> :first-child) {
  margin-top: 0;
}

.texto-resposta :deep(> :last-child) {
  margin-bottom: 0;
}

.texto-resposta :deep(p),
.texto-resposta :deep(ul),
.texto-resposta :deep(ol),
.texto-resposta :deep(pre),
.texto-resposta :deep(blockquote),
.texto-resposta :deep(table) {
  margin: 0 0 10px;
}

.texto-resposta :deep(ul),
.texto-resposta :deep(ol) {
  padding-left: 20px;
}

.texto-resposta :deep(ul) {
  list-style: disc;
}

.texto-resposta :deep(ol) {
  list-style: decimal;
}

.texto-resposta :deep(li + li) {
  margin-top: 3px;
}

.texto-resposta :deep(h1),
.texto-resposta :deep(h2),
.texto-resposta :deep(h3),
.texto-resposta :deep(h4) {
  margin: 14px 0 6px;
  font-size: 15px;
  font-weight: 700;
}

.texto-resposta :deep(strong) {
  font-weight: 700;
}

.texto-resposta :deep(a) {
  color: #2563eb;
  text-decoration: underline;
}

.texto-resposta :deep(code) {
  padding: 1px 5px;
  border-radius: 5px;
  background: #e2e8f0;
  font-size: 12.5px;
}

.texto-resposta :deep(pre) {
  padding: 10px 12px;
  border-radius: 8px;
  background: #0f172a;
  color: #e2e8f0;
  overflow-x: auto;
}

.texto-resposta :deep(pre code) {
  padding: 0;
  background: transparent;
  color: inherit;
}

.texto-resposta :deep(blockquote) {
  padding-left: 10px;
  border-left: 3px solid #cbd5e1;
  color: #475569;
}

/* tabelas largas rolam na horizontal em vez de quebrar as palavras no meio */
.texto-resposta :deep(table) {
  display: block;
  border-collapse: collapse;
  overflow-x: auto;
  overflow-wrap: normal;
  font-size: 13px;
}

.texto-resposta :deep(th),
.texto-resposta :deep(td) {
  padding: 4px 8px;
  border: 1px solid #e2e8f0;
}

.falha {
  display: grid;
  justify-items: start;
  gap: 6px;
  padding: 10px 12px;
  border: 1px solid #fecaca;
  border-radius: 10px;
  background: #fef2f2;
  font-size: 13px;
}

.falha p {
  margin: 0;
}

.pergunta-falha {
  color: #334155;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.mensagem-erro {
  color: #b91c1c;
}

.tentar-de-novo {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #b91c1c;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.composicao {
  display: grid;
  gap: 6px;
  padding: 10px 12px 12px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
}

.chip-foco {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  justify-self: start;
  max-width: 100%;
  padding: 2px 4px 2px 9px;
  border-radius: 999px;
  background: #ede9fe;
  color: #5b21b6;
  font-size: 12px;
  font-weight: 600;
}

.rotulo-foco {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chip-foco button {
  display: grid;
  flex: 0 0 auto;
  width: 18px;
  height: 18px;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.chip-foco button:hover {
  background: #ddd6fe;
}

.campo-e-botao {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 6px 6px 6px 10px;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  background: #fff;
}

.campo-e-botao:focus-within {
  border-color: #93c5fd;
  box-shadow: 0 0 0 3px rgb(37 99 235 / 12%);
}

textarea {
  flex: 1;
  min-width: 0;
  max-height: 160px;
  padding: 5px 0;
  border: 0;
  outline: none;
  background: transparent;
  color: #0f172a;
  font: inherit;
  font-size: 14px;
  line-height: 1.4;
  resize: none;
}

textarea:disabled {
  cursor: not-allowed;
}

.botao-enviar {
  display: grid;
  flex: 0 0 32px;
  width: 32px;
  height: 32px;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: #2563eb;
  color: #fff;
  cursor: pointer;
}

.botao-enviar:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
}

.aviso {
  margin: 0;
  color: #94a3b8;
  font-size: 11px;
  line-height: 1.35;
}

.botao-icone:focus-visible,
.sugestao:focus-visible,
.tentar-de-novo:focus-visible,
.chip-foco button:focus-visible,
.botao-enviar:focus-visible {
  outline: 2px solid rgb(37 99 235 / 55%);
  outline-offset: 2px;
}

/* em telas estreitas, o assistente é uma gaveta sobre a página, e recolhido vira um botão flutuante */
@media (max-width: 900px) {
  .assistente {
    position: fixed;
    top: 0;
    right: 0;
    bottom: 0;
    z-index: 40;
    width: min(400px, 100%);
    box-shadow: -12px 0 32px rgb(15 23 42 / 18%);
  }

  .recolhido {
    top: auto;
    right: 16px;
    bottom: 16px;
    width: 52px;
    height: 52px;
    padding: 0;
    border: 0;
    border-radius: 999px;
    background: #7c3aed;
    box-shadow: 0 8px 24px rgb(76 29 149 / 35%);
  }

  .recolhido .botao-icone {
    width: 52px;
    height: 52px;
    border-radius: 999px;
    color: #fff;
  }

  .recolhido .botao-icone:hover {
    border-color: transparent;
    background: rgb(255 255 255 / 12%);
    color: #fff;
  }

  .recolhido .icone-trilho,
  .recolhido .marca {
    display: none;
  }

  .recolhido .icone-flutuante {
    display: block;
  }
}
</style>
