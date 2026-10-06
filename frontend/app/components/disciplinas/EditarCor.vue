<script setup lang="ts">
import type { Disciplina } from '~/types/disciplina'
import { Palette } from '@lucide/vue'

const props = defineProps<{
  disciplina: Disciplina
}>()

const { atualizarCor } = useDisciplinas()
const cores = ['#2563eb', '#7c3aed', '#db2777', '#dc2626', '#ea580c', '#16a34a', '#0891b2', '#334155']
const aberta = ref(false)
const cor = ref(props.disciplina.cor || cores[0])
const hex = ref(cor.value.toUpperCase())
const gatilho = ref<HTMLButtonElement | null>(null)
const popover = ref<HTMLDivElement | null>(null)
const colorPickerRoot = ref<HTMLDivElement | null>(null)
const posicao = ref<Record<string, string>>({})

type ColorValue = { hexString: string }
type ColorPickerInstance = {
  color: ColorValue
  on: (event: string, callback: (color: ColorValue) => void) => void
  off: (event: string, callback: (color: ColorValue) => void) => void
}

let colorPicker: ColorPickerInstance | null = null
let onColorChange: ((color: ColorValue) => void) | null = null

watch(cor, (value) => {
  hex.value = value.toUpperCase()
  props.disciplina.cor = value

  if (colorPicker && colorPicker.color && colorPicker.color.hexString !== value) {
    colorPicker.color.hexString = value
  }
})

function salvarCor(value: string) {
  cor.value = value
  atualizarCor(props.disciplina.id, value)
}

function posicionar() {
  if (!gatilho.value || !popover.value) return

  const anchor = gatilho.value.getBoundingClientRect()
  const width = Math.min(276, window.innerWidth - 24)
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

  await nextTick()
  posicionar()

  if (colorPicker) {
    colorPicker.color.hexString = cor.value
    return
  }

  if (!colorPickerRoot.value) return
  const { default: iro } = await import('@jaames/iro')
  colorPicker = new iro.ColorPicker(colorPickerRoot.value, {
    width: 220,
    color: cor.value,
    borderWidth: 1,
    borderColor: '#e2e8f0',
    layout: [
      { component: iro.ui.Box },
      { component: iro.ui.Slider, options: { sliderType: 'hue' } }
    ]
  })

  onColorChange = (value) => salvarCor(value.hexString)
  colorPicker.on('color:change', onColorChange)
  await nextTick()
  posicionar()
}

function aplicarHex() {
  if (/^#[0-9a-f]{6}$/i.test(hex.value.trim())) {
    salvarCor(hex.value.trim())
  }
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
  if (colorPicker && onColorChange) colorPicker.off('color:change', onColorChange)
})
</script>

<template>
  <button
    ref="gatilho"
    type="button"
    class="botao-personalizacao"
    aria-label="Editar cor da disciplina"
    title="Editar cor"
    :aria-expanded="aberta"
    @click="alternar"
  >
    <Palette :size="14" :stroke-width="1.8" aria-hidden="true" />
    <span class="indicador-cor" :style="{ background: cor }" aria-hidden="true" />
  </button>

  <Teleport to="body">
    <div
      v-show="aberta"
      ref="popover"
      class="popover-cor"
      :style="posicao"
      @click.stop
    >
      <div class="cabecalho-cor">
        <span>Cor</span>
      </div>

      <div class="cores-rapidas" aria-label="Cores sugeridas">
        <button
          v-for="opcao in cores"
          :key="opcao"
          type="button"
          class="amostra-cor"
          :class="{ selecionada: cor.toLowerCase() === opcao }"
          :style="{ background: opcao }"
          :aria-label="`Selecionar cor ${opcao}`"
          :aria-pressed="cor.toLowerCase() === opcao"
          :title="opcao"
          @click="salvarCor(opcao)"
        >
          <span v-if="cor.toLowerCase() === opcao" aria-hidden="true">✓</span>
        </button>
      </div>

      <div ref="colorPickerRoot" class="seletor-espectro" />

      <div class="linha-hex">
        <span class="previa-hex" :style="{ background: cor }" />
        <label for="editar-cor-hex">HEX</label>
        <input
          id="editar-cor-hex"
          v-model="hex"
          type="text"
          maxlength="7"
          aria-label="Código hexadecimal da cor"
          @input="aplicarHex"
          @blur="aplicarHex"
        >
      </div>
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

.indicador-cor {
  position: absolute;
  right: 1px;
  bottom: 1px;
  width: 7px;
  height: 7px;
  border: 1px solid #fff;
  border-radius: 50%;
}

.popover-cor {
  position: fixed;
  z-index: 1000;
  width: min(300px, calc(100vw - 24px));
  max-height: calc(100vh - 24px);
  overflow-y: auto;
  padding: 12px 12px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 18px 38px rgb(15 23 42 / 16%);
}

.cabecalho-cor {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #475569;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.cores-rapidas {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 4px 12px;
  border-bottom: 1px solid #e2e8f0;
}

.amostra-cor {
  display: grid;
  width: 26px;
  height: 26px;
  place-items: center;
  border: 1px solid rgb(15 23 42 / 10%);
  border-radius: 50%;
  color: #fff;
  font-size: 12px;
  cursor: pointer;
  transition: transform 120ms ease, box-shadow 120ms ease;
}

.amostra-cor:hover {
  transform: translateY(-1px);
}

.amostra-cor.selecionada {
  box-shadow: 0 0 0 2px #fff, 0 0 0 3px #0f172a;
}

.seletor-espectro {
  display: flex;
  justify-content: center;
  padding: 12px 0 8px;
}

.linha-hex {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 2px;
  padding: 6px 8px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.previa-hex {
  width: 18px;
  height: 18px;
  flex: 0 0 18px;
  border: 1px solid rgba(15, 23, 42, 0.15);
  border-radius: 50%;
}

.linha-hex label {
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
}

.linha-hex input {
  width: 100%;
  min-width: 0;
  height: 30px;
  padding: 0 8px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #334155;
  font-family: monospace;
  font-size: 12px;
  outline: none;
}

.linha-hex input:focus {
  background: #fff;
}

.seletor-espectro :deep(svg) {
  max-width: 100%;
  border-radius: 10px;
}
</style>