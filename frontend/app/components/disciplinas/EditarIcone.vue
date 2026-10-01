<script setup lang="ts">
import type { Disciplina } from '~/types/disciplina'
import { Sticker } from '@lucide/vue'

const props = defineProps<{
  disciplina: Disciplina
}>()

const { atualizarIcone } = useDisciplinas()
const aberta = ref(false)
const gatilho = ref<HTMLButtonElement | null>(null)
const popover = ref<HTMLDivElement | null>(null)
const emojiPickerRoot = ref<HTMLDivElement | null>(null)
const posicao = ref<Record<string, string>>({})
let emojiPickerElement: HTMLElement | null = null

function mudar(icone: string) {
  atualizarIcone(
    props.disciplina.id,
    icone
  )

  props.disciplina.icone = icone
  aberta.value = false
}

function posicionar() {
  if (!gatilho.value || !popover.value) return

  const anchor = gatilho.value.getBoundingClientRect()
  const width = Math.min(360, window.innerWidth - 24)
  const height = Math.min(popover.value.offsetHeight || 370, window.innerHeight - 24)
  const top = window.innerHeight - anchor.bottom - 8 >= height
    ? anchor.bottom + 8
    : Math.max(12, anchor.top - height - 8)
  const left = Math.max(12, Math.min(anchor.left, window.innerWidth - width - 12))

  posicao.value = { top: `${top}px`, left: `${left}px` }
}

async function alternar() {
  aberta.value = !aberta.value
  if (!aberta.value) return

  await import('emoji-picker-element')
  await nextTick()
  posicionar()

  const root = emojiPickerRoot.value
  if (!root || root.childElementCount) return

  emojiPickerElement = document.createElement('emoji-picker')
  emojiPickerElement.setAttribute('locale', 'pt_BR')
  emojiPickerElement.addEventListener('emoji-click', (evento: Event) => {
    const emoji = (evento as CustomEvent<{ unicode?: string }>).detail?.unicode
    if (emoji) mudar(emoji)
  })
  root.appendChild(emojiPickerElement)
}

function aoClicarFora(event: PointerEvent) {
  if (event.target instanceof Node
    && !popover.value?.contains(event.target)
    && !gatilho.value?.contains(event.target)) {
    aberta.value = false
  }
}

function aoPressionarTecla(event: KeyboardEvent) {
  if (event.key === 'Escape') aberta.value = false
}

onMounted(() => {
  window.addEventListener('pointerdown', aoClicarFora)
  window.addEventListener('keydown', aoPressionarTecla)
  window.addEventListener('resize', posicionar)
  window.addEventListener('scroll', posicionar, true)
})

onBeforeUnmount(() => {
  window.removeEventListener('pointerdown', aoClicarFora)
  window.removeEventListener('keydown', aoPressionarTecla)
  window.removeEventListener('resize', posicionar)
  window.removeEventListener('scroll', posicionar, true)
})
</script>

<template>
  <button
    ref="gatilho"
    type="button"
    class="botao-personalizacao"
    aria-label="Editar ícone da disciplina"
    title="Editar ícone"
    :aria-expanded="aberta"
    @click="alternar"
  >
    <Sticker :size="14" :stroke-width="1.8" aria-hidden="true" />
    <span v-if="disciplina.icone" class="indicador-icone" aria-hidden="true">
      {{ disciplina.icone }}
    </span>
  </button>

  <Teleport to="body">
    <div
      v-show="aberta"
      ref="popover"
      class="popover-emoji"
      :style="posicao"
      @click.stop
    >
      <div ref="emojiPickerRoot" class="seletor-emoji" />
    </div>
  </Teleport>
</template>

<style scoped>
.botao-personalizacao {
  position: relative;
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  padding: 0;
  border: 1px solid #e2e8f0;
  border-radius: 5px;
  background: #fff;
  color: #64748b;
  cursor: pointer;
}

.botao-personalizacao:hover,
.botao-personalizacao[aria-expanded="true"] {
  border-color: #cbd5e1;
  background: #f8fafc;
}

.indicador-icone {
  position: absolute;
  right: 0;
  bottom: 0;
  display: grid;
  width: 9px;
  height: 9px;
  place-items: center;
  overflow: hidden;
  border: 1px solid #fff;
  border-radius: 50%;
  background: #fff;
  font-size: 7px;
  line-height: 1;
}

.popover-emoji {
  position: fixed;
  z-index: 1000;
  width: min(360px, calc(100vw - 24px));
  height: min(360px, calc(100vh - 24px));
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 12px 28px rgb(15 23 42 / 18%);
}

.seletor-emoji :deep(emoji-picker) {
  display: block;
  width: 100%;
  height: 100%;
  --border-size: 0;
  --border-radius: 8px;
  --num-columns: 8;
  --emoji-size: 1.45rem;
  --emoji-padding: 0.45rem;
  --background: #fff;
  --indicator-color: #2563eb;
  --button-hover-background: #eff6ff;
}
</style>