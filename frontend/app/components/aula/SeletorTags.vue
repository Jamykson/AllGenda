<script setup lang="ts">
import { Check, MoreHorizontal, Plus, Trash2, X } from '@lucide/vue'
import type { Tag } from '~/types/anotacao'

// Seletor de tags da anotação, no estilo da seleção múltipla do Notion. O campo mostra as tags
// escolhidas; clicar nele abre uma caixa com busca, as tags da disciplina e "Criar" para um nome novo.
// O "⋯" de cada opção renomeia ou exclui a tag na disciplina inteira. As ações chegam como funções,
// para que os erros da API (como um nome repetido) apareçam aqui mesmo.

type ItemDaLista = { tipo: 'tag', tag: Tag } | { tipo: 'criar', nome: string }

const LARGURA_CAIXA = 300

const props = defineProps<{
  selecionadas: string[]
  opcoes: Tag[] // as tags da disciplina, em ordem alfabética
  desabilitado?: boolean
  dicaDesabilitado?: string
  acrescentar: (nome: string) => Promise<void>
  remover: (nome: string) => Promise<void>
  renomear: (tag: Tag, nome: string) => Promise<void>
  excluir: (tag: Tag) => Promise<void>
}>()

const aberto = ref(false)
const busca = ref('')
const indiceDestacado = ref(0)
const erro = ref('')
const campo = ref<HTMLElement | null>(null)
const caixa = ref<HTMLElement | null>(null)
const campoBusca = ref<HTMLInputElement | null>(null)
const posicaoCaixa = ref<Record<string, string>>({ visibility: 'hidden' })

// submenu do "⋯": renomear e excluir a tag
const menu = ref<{ tag: Tag, nome: string, erro: string } | null>(null)
const menuEl = ref<HTMLElement | null>(null)
const campoNome = ref<HTMLInputElement | null>(null)
const posicaoMenu = ref<Record<string, string>>({ visibility: 'hidden' })
let ancoraMenu: HTMLElement | null = null

// as ações vão uma de cada vez, na ordem em que a pessoa as fez
let fila: Promise<void> = Promise.resolve()

function normalizar(texto: string) {
  return texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
}

function estaSelecionada(nome: string) {
  return props.selecionadas.some(item => item.toLowerCase() === nome.toLowerCase())
}

const itens = computed<ItemDaLista[]>(() => {
  const termo = busca.value.trim()
  const lista: ItemDaLista[] = props.opcoes
    .filter(tag => normalizar(tag.nome).includes(normalizar(termo)))
    .map(tag => ({ tipo: 'tag', tag }))
  const existe = props.opcoes.some(tag => tag.nome.toLowerCase() === termo.toLowerCase())
  if (termo && !existe) lista.push({ tipo: 'criar', nome: termo })
  return lista
})

watch(itens, () => {
  indiceDestacado.value = Math.min(indiceDestacado.value, Math.max(itens.value.length - 1, 0))
})

watch(busca, () => {
  indiceDestacado.value = 0
})

function executar(acao: () => Promise<void>, mensagemPadrao: string) {
  erro.value = ''
  fila = fila.then(acao).catch((falha) => {
    console.error('[tags] erro:', falha)
    erro.value = mensagemDeErro(falha, mensagemPadrao)
  })
  return fila
}

function escolher(item: ItemDaLista | undefined) {
  if (!item) return
  if (item.tipo === 'criar') {
    void executar(() => props.acrescentar(item.nome), 'Não foi possível criar a tag.')
  }
  else if (estaSelecionada(item.tag.nome)) {
    void executar(() => props.remover(item.tag.nome), 'Não foi possível remover a tag.')
  }
  else {
    void executar(() => props.acrescentar(item.tag.nome), 'Não foi possível acrescentar a tag.')
  }
  busca.value = ''
  campoBusca.value?.focus()
}

function tirar(nome: string) {
  void executar(() => props.remover(nome), 'Não foi possível remover a tag.')
}

function aoPressionarTecla(evento: KeyboardEvent) {
  const total = itens.value.length
  if (evento.key === 'ArrowDown' && total) {
    evento.preventDefault()
    indiceDestacado.value = (indiceDestacado.value + 1) % total
    rolarParaDestacado()
  }
  else if (evento.key === 'ArrowUp' && total) {
    evento.preventDefault()
    indiceDestacado.value = (indiceDestacado.value - 1 + total) % total
    rolarParaDestacado()
  }
  else if (evento.key === 'Enter') {
    evento.preventDefault()
    escolher(itens.value[indiceDestacado.value])
  }
  else if (evento.key === 'Backspace' && !busca.value && props.selecionadas.length) {
    tirar(props.selecionadas[props.selecionadas.length - 1]!)
  }
  else if (evento.key === 'Escape') {
    evento.preventDefault()
    evento.stopPropagation()
    fechar()
  }
  else if (evento.key === 'Tab') {
    fechar()
  }
}

async function rolarParaDestacado() {
  await nextTick()
  caixa.value?.querySelector('.opcao.destacada')?.scrollIntoView({ block: 'nearest' })
}

// a caixa muda de altura conforme a lista filtra ou cresce; reposiciona para continuar dentro da tela
let observador: ResizeObserver | null = null

async function abrir() {
  if (props.desabilitado || aberto.value) return
  aberto.value = true
  busca.value = ''
  erro.value = ''
  indiceDestacado.value = 0
  posicaoCaixa.value = { visibility: 'hidden' }
  await nextTick()
  posicionarCaixa()
  await nextTick() // um elemento ainda invisível não recebe foco
  campoBusca.value?.focus()
  if (caixa.value && typeof ResizeObserver !== 'undefined') {
    observador = new ResizeObserver(() => posicionarCaixa())
    observador.observe(caixa.value)
  }
  window.addEventListener('resize', reposicionar)
  window.addEventListener('scroll', reposicionar, true)
  document.addEventListener('pointerdown', aoClicarFora)
}

function fechar() {
  if (menu.value) void confirmarNome()
  menu.value = null
  aberto.value = false
  observador?.disconnect()
  observador = null
  window.removeEventListener('resize', reposicionar)
  window.removeEventListener('scroll', reposicionar, true)
  document.removeEventListener('pointerdown', aoClicarFora)
}

// a caixa flutua sobre o começo do campo, como no Notion, com largura própria e compacta
// (não acompanha a largura do campo); se não couber abaixo, sobe até caber
function posicionarCaixa() {
  if (!campo.value || !caixa.value) return
  const ancora = campo.value.getBoundingClientRect()
  const margem = 8
  const largura = Math.min(LARGURA_CAIXA, window.innerWidth - margem * 2)
  const altura = caixa.value.offsetHeight
  const left = Math.max(margem, Math.min(ancora.left, window.innerWidth - largura - margem))
  const top = Math.max(margem, Math.min(ancora.top, window.innerHeight - altura - margem))
  posicaoCaixa.value = { top: `${top}px`, left: `${left}px`, width: `${largura}px`, visibility: 'visible' }
}

// o submenu fica ao lado do "⋯"; sem espaço à direita, vai para a esquerda
function posicionarMenu() {
  if (!ancoraMenu || !menuEl.value) return
  const botao = ancoraMenu.getBoundingClientRect()
  const margem = 8
  const largura = menuEl.value.offsetWidth
  const altura = menuEl.value.offsetHeight
  const cabeADireita = botao.right + 6 + largura <= window.innerWidth - margem
  const left = cabeADireita ? botao.right + 6 : Math.max(margem, botao.left - largura - 6)
  const top = Math.max(margem, Math.min(botao.top - 4, window.innerHeight - altura - margem))
  posicaoMenu.value = { top: `${top}px`, left: `${left}px`, visibility: 'visible' }
}

function reposicionar() {
  posicionarCaixa()
  posicionarMenu()
}

function aoClicarFora(evento: PointerEvent) {
  const alvo = evento.target
  if (!(alvo instanceof Node)) return
  if (menu.value && !menuEl.value?.contains(alvo)) {
    void confirmarNome()
    menu.value = null
    if (caixa.value?.contains(alvo)) return
  }
  if (!caixa.value?.contains(alvo) && !menuEl.value?.contains(alvo) && !campo.value?.contains(alvo)) {
    fechar()
  }
}

async function abrirMenu(tag: Tag, evento: MouseEvent) {
  if (menu.value?.tag.id === tag.id) {
    menu.value = null
    return
  }
  ancoraMenu = evento.currentTarget as HTMLElement
  menu.value = { tag, nome: tag.nome, erro: '' }
  posicaoMenu.value = { visibility: 'hidden' }
  await nextTick()
  posicionarMenu()
  await nextTick()
  campoNome.value?.focus()
  campoNome.value?.select()
}

// renomeia ao confirmar o campo (Enter ou ao sair dele); um nome igual ou vazio não muda nada
async function confirmarNome() {
  const atual = menu.value
  if (!atual) return
  const nome = atual.nome.trim()
  if (!nome || nome === atual.tag.nome) return

  atual.erro = ''
  try {
    await props.renomear(atual.tag, nome)
    if (menu.value === atual) {
      menu.value = null
      campoBusca.value?.focus()
    }
  }
  catch (falha) {
    console.error('[tags] erro ao renomear:', falha)
    const mensagem = mensagemDeErro(falha, 'Não foi possível renomear a tag.')
    if (menu.value === atual) atual.erro = mensagem
    else erro.value = mensagem
  }
}

function cancelarMenu() {
  menu.value = null
  campoBusca.value?.focus()
}

async function excluirTagDoMenu() {
  const atual = menu.value
  if (!atual) return
  const confirmado = window.confirm(
    `Excluir a tag "${atual.tag.nome}"? Ela será removida de todas as anotações desta disciplina.`
  )
  if (!confirmado) return

  atual.erro = ''
  try {
    await props.excluir(atual.tag)
    menu.value = null
    campoBusca.value?.focus()
  }
  catch (falha) {
    console.error('[tags] erro ao excluir:', falha)
    atual.erro = mensagemDeErro(falha, 'Não foi possível excluir a tag.')
  }
}

onBeforeUnmount(fechar)
</script>

<template>
  <div class="seletor-tags">
    <div
      ref="campo"
      class="campo-tags"
      :class="{ desabilitado, aberto }"
      :title="desabilitado ? dicaDesabilitado : undefined"
      @click="abrir"
    >
      <ul v-if="selecionadas.length" class="chips" aria-label="Tags da anotação">
        <li v-for="nome in selecionadas" :key="nome" class="chip">
          {{ nome }}
          <button
            type="button"
            class="tirar"
            :aria-label="`Remover tag ${nome}`"
            :title="`Remover tag ${nome}`"
            :disabled="desabilitado"
            @click.stop="tirar(nome)"
          >
            <X :size="12" :stroke-width="2" aria-hidden="true" />
          </button>
        </li>
      </ul>
      <button
        type="button"
        class="abrir-tags"
        aria-haspopup="dialog"
        :aria-expanded="aberto"
        :disabled="desabilitado"
        @click.stop="abrir"
      >
        <Plus :size="14" :stroke-width="2" aria-hidden="true" />
        {{ selecionadas.length ? 'Tag' : 'Acrescentar tag' }}
      </button>
      <span v-if="desabilitado && dicaDesabilitado" class="dica-desabilitado">{{ dicaDesabilitado }}</span>
    </div>

    <Teleport to="body">
      <div
        v-if="aberto"
        ref="caixa"
        class="caixa-tags"
        role="dialog"
        aria-label="Tags da anotação"
        :style="posicaoCaixa"
      >
        <div class="topo-caixa" @click="campoBusca?.focus()">
          <span v-for="nome in selecionadas" :key="nome" class="chip">
            {{ nome }}
            <button type="button" class="tirar" :aria-label="`Remover tag ${nome}`" @click.stop="tirar(nome)">
              <X :size="12" :stroke-width="2" aria-hidden="true" />
            </button>
          </span>
          <input
            ref="campoBusca"
            v-model="busca"
            type="text"
            class="campo-busca"
            role="combobox"
            aria-label="Procurar ou criar tag"
            aria-autocomplete="list"
            aria-controls="opcoes-tags"
            :aria-expanded="true"
            :aria-activedescendant="itens.length ? `opcao-tag-${indiceDestacado}` : undefined"
            @keydown="aoPressionarTecla"
          />
        </div>

        <p class="dica">Selecione uma opção ou crie uma</p>

        <ul id="opcoes-tags" class="opcoes" role="listbox" aria-label="Tags da disciplina" aria-multiselectable="true">
          <li
            v-for="(item, indice) in itens"
            :id="`opcao-tag-${indice}`"
            :key="item.tipo === 'tag' ? item.tag.id : 'criar'"
            class="opcao"
            :class="{ destacada: indice === indiceDestacado }"
            role="option"
            :aria-selected="item.tipo === 'tag' && estaSelecionada(item.tag.nome)"
            @mouseenter="indiceDestacado = indice"
            @mousedown.prevent
            @click="escolher(item)"
          >
            <template v-if="item.tipo === 'criar'">
              <span class="rotulo-criar">Criar</span>
              <span class="chip">{{ item.nome }}</span>
            </template>
            <template v-else>
              <span class="chip">{{ item.tag.nome }}</span>
              <Check
                v-if="estaSelecionada(item.tag.nome)"
                class="marcada"
                :size="15"
                :stroke-width="2"
                aria-hidden="true"
              />
              <button
                type="button"
                class="mais"
                :class="{ ativo: menu?.tag.id === item.tag.id }"
                :aria-label="`Opções da tag ${item.tag.nome}`"
                title="Renomear ou excluir"
                @mousedown.stop.prevent
                @click.stop="abrirMenu(item.tag, $event)"
              >
                <MoreHorizontal :size="15" :stroke-width="2" aria-hidden="true" />
              </button>
            </template>
          </li>
        </ul>
        <p v-if="!itens.length" class="vazio">Digite um nome para criar a primeira tag desta disciplina.</p>
        <p v-if="erro" class="mensagem-erro" role="alert">{{ erro }}</p>
      </div>

      <div
        v-if="aberto && menu"
        ref="menuEl"
        class="menu-tag"
        role="dialog"
        :aria-label="`Opções da tag ${menu.tag.nome}`"
        :style="posicaoMenu"
      >
        <input
          ref="campoNome"
          v-model="menu.nome"
          type="text"
          class="campo-nome"
          aria-label="Nome da tag"
          maxlength="255"
          @keydown.enter.prevent="confirmarNome"
          @keydown.esc.stop.prevent="cancelarMenu"
          @blur="confirmarNome"
        />
        <button type="button" class="excluir" @mousedown.prevent @click="excluirTagDoMenu">
          <Trash2 :size="15" :stroke-width="1.8" aria-hidden="true" />
          Excluir
        </button>
        <p v-if="menu.erro" class="mensagem-erro" role="alert">{{ menu.erro }}</p>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.seletor-tags {
  min-width: 0;
}

.campo-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  min-height: 36px;
  padding: 4px 6px;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
}

.campo-tags:hover:not(.desabilitado) {
  background: #f1f5f9;
}

.campo-tags.desabilitado {
  cursor: default;
}

.chips {
  display: contents;
  margin: 0;
  padding: 0;
  list-style: none;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  max-width: 100%;
  min-height: 22px;
  padding: 1px 6px;
  border-radius: 4px;
  background: #e8eaee;
  color: #1f2937;
  font-size: 13px;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.tirar {
  display: grid;
  width: 18px;
  height: 18px;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 3px;
  background: transparent;
  color: #6b7280;
  cursor: pointer;
}

.tirar:hover {
  background: #d1d5db;
  color: #111827;
}

.abrir-tags {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 26px;
  padding: 0 8px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #64748b;
  font-size: 13px;
  cursor: pointer;
}

.abrir-tags:hover:not(:disabled) {
  background: #e2e8f0;
  color: #1f2937;
}

.abrir-tags:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.dica-desabilitado {
  color: #94a3b8;
  font-size: 12px;
}

.caixa-tags {
  position: fixed;
  z-index: 100;
  display: flex;
  flex-direction: column;
  max-height: min(380px, calc(100dvh - 16px));
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 24px rgb(15 23 42 / 14%);
  box-sizing: border-box;
}

.topo-caixa {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  border-bottom: 1px solid #ececec;
  background: #f7f7f5;
  cursor: text;
}

.campo-busca {
  flex: 1 0 80px;
  min-width: 80px;
  height: 24px;
  padding: 0;
  border: 0;
  outline: none;
  background: transparent;
  color: #111827;
  font: inherit;
  font-size: 14px;
}

.dica {
  margin: 0;
  padding: 10px 12px 4px;
  color: #6b7280;
  font-size: 12px;
}

.opcoes {
  flex: 1;
  min-height: 0;
  margin: 0;
  padding: 2px 4px 6px;
  overflow-y: auto;
  list-style: none;
}

.opcao {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 30px;
  padding: 3px 8px;
  border-radius: 6px;
  cursor: pointer;
}

.opcao.destacada {
  background: #f1f1ef;
}

.rotulo-criar {
  color: #374151;
  font-size: 14px;
}

.marcada {
  color: #2563eb;
}

.mais {
  display: grid;
  width: 26px;
  height: 24px;
  margin-left: auto;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: #6b7280;
  opacity: 0;
  cursor: pointer;
}

.opcao.destacada .mais,
.mais.ativo,
.mais:focus-visible {
  opacity: 1;
}

.mais:hover,
.mais.ativo {
  background: #e5e7eb;
  color: #111827;
}

@media (hover: none) {
  .mais {
    opacity: 1;
  }
}

.vazio {
  margin: 0;
  padding: 6px 12px 10px;
  color: #94a3b8;
  font-size: 13px;
}

.mensagem-erro {
  margin: 0;
  padding: 6px 12px 10px;
  color: #b91c1c;
  font-size: 13px;
}

.menu-tag {
  position: fixed;
  z-index: 101;
  display: grid;
  gap: 4px;
  width: 220px;
  padding: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 24px rgb(15 23 42 / 14%);
}

.campo-nome {
  height: 32px;
  padding: 0 8px;
  border: 1px solid #93c5fd;
  border-radius: 6px;
  outline: 2px solid rgb(37 99 235 / 20%);
  color: #111827;
  font: inherit;
  font-size: 14px;
}

.excluir {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  padding: 0 8px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #374151;
  font: inherit;
  font-size: 14px;
  text-align: left;
  cursor: pointer;
}

.excluir:hover {
  background: #f1f1ef;
  color: #b91c1c;
}

.menu-tag .mensagem-erro {
  padding: 2px 4px 0;
}

.abrir-tags:focus-visible,
.tirar:focus-visible,
.mais:focus-visible,
.excluir:focus-visible {
  outline: 2px solid rgb(37 99 235 / 55%);
  outline-offset: 1px;
}
</style>
