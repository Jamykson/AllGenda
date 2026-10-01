<script setup lang="ts">
import type { NovaDisciplina } from '~/types/disciplina'
import { Palette, Sticker } from '@lucide/vue'

interface HorarioRecorrenteForm {
  dias: number[]
  horaInicio: string
  horaFim: string
  dataInicio: string
  dataFim: string
}

const props = withDefaults(defineProps<{
  salvando?: boolean
  erro?: string
}>(), {
  salvando: false,
  erro: ''
})

const emit = defineEmits<{
  cancelar: []
  salvar: [disciplina: NovaDisciplina]
}>()

const cores = [
  '#2563eb',
  '#7c3aed',
  '#db2777',
  '#dc2626',
  '#ea580c',
  '#16a34a',
  '#0891b2',
  '#334155'
]
const nome = ref('')
const cor = ref(cores[0])
const icone = ref('📚')
const hexManual = ref(cor.value)
const diasSemana = [
  { numero: 0, nome: 'Dom' },
  { numero: 1, nome: 'Seg' },
  { numero: 2, nome: 'Ter' },
  { numero: 3, nome: 'Qua' },
  { numero: 4, nome: 'Qui' },
  { numero: 5, nome: 'Sex' },
  { numero: 6, nome: 'Sáb' }
]
const horario = ref<HorarioRecorrenteForm>({
  dias: [],
  horaInicio: '',
  horaFim: '',
  dataInicio: '',
  dataFim: ''
})
const erroHorario = ref('')
const seletorCorAberto = ref(false)
const seletorEmojiAberto = ref(false)
const erroValidacao = ref('')
const campoNome = ref<HTMLInputElement | null>(null)
const corTrigger = ref<HTMLButtonElement | null>(null)
const emojiTrigger = ref<HTMLButtonElement | null>(null)
const popoverCorRoot = ref<HTMLDivElement | null>(null)
const popoverEmojiRoot = ref<HTMLDivElement | null>(null)
const colorPickerRoot = ref<HTMLDivElement | null>(null)
const emojiPickerRoot = ref<HTMLDivElement | null>(null)
const popoverCorPosition = ref<Record<string, string>>({})
const popoverEmojiPosition = ref<Record<string, string>>({})
const horarioValido = computed(() => {
  const { dias, horaInicio, horaFim, dataInicio, dataFim } = horario.value

  return dias.length > 0
    && !!horaInicio
    && !!horaFim
    && !!dataInicio
    && !!dataFim
    && horaFim > horaInicio
    && dataFim >= dataInicio
})
type ColorValue = { hexString: string }
type ColorPickerInstance = {
  color: ColorValue
  on: (event: string, callback: (color: ColorValue) => void) => void
  off: (event: string, callback: (color: ColorValue) => void) => void
}
let colorPicker: ColorPickerInstance | null = null
let colorPickerChange: ((color: ColorValue) => void) | null = null
let emojiPickerElement: HTMLElement | null = null
let overflowAnterior = ''

watch(cor, (valor) => {
  hexManual.value = valor.toUpperCase()
  if (colorPicker && colorPicker.color.hexString.toLowerCase() !== valor.toLowerCase()) {
    colorPicker.color.hexString = valor
  }
})

function fechar() {
  if (!props.salvando) {
    emit('cancelar')
  }
}

function aoPressionarTecla(evento: KeyboardEvent) {
  if (evento.key === 'Escape') {
    if (seletorCorAberto.value || seletorEmojiAberto.value) {
      seletorCorAberto.value = false
      seletorEmojiAberto.value = false
    }
    else {
      fechar()
    }
  }
}

function posicionarPopover(
  gatilho: HTMLElement | null,
  popover: HTMLElement | null,
  larguraMinima: number
) {
  if (!gatilho || !popover) return {}

  const ancora = gatilho.getBoundingClientRect()
  const largura = Math.min(larguraMinima, window.innerWidth - 24)
  const altura = Math.min(popover.offsetHeight || 380, window.innerHeight - 24)
  const abaixo = window.innerHeight - ancora.bottom - 8 >= altura
  const top = abaixo
    ? ancora.bottom + 8
    : Math.max(12, ancora.top - altura - 8)
  const left = Math.max(12, Math.min(ancora.left, window.innerWidth - largura - 12))

  return {
    top: `${top}px`,
    left: `${left}px`,
    maxHeight: `calc(100vh - 24px)`
  }
}

function reposicionarPopovers() {
  if (seletorCorAberto.value) {
    popoverCorPosition.value = posicionarPopover(
      corTrigger.value,
      popoverCorRoot.value,
      276
    )
  }

  if (seletorEmojiAberto.value) {
    popoverEmojiPosition.value = posicionarPopover(
      emojiTrigger.value,
      popoverEmojiRoot.value,
      360
    )
  }
}

function aoClicarFora(evento: PointerEvent) {
  const alvo = evento.target

  if (alvo instanceof Node) {
    if (seletorCorAberto.value
      && !popoverCorRoot.value?.contains(alvo)
      && !corTrigger.value?.contains(alvo)) {
      seletorCorAberto.value = false
    }

    if (seletorEmojiAberto.value
      && !popoverEmojiRoot.value?.contains(alvo)
      && !emojiTrigger.value?.contains(alvo)) {
      seletorEmojiAberto.value = false
    }
  }
}

async function abrirSeletorCor() {
  seletorEmojiAberto.value = false
  seletorCorAberto.value = !seletorCorAberto.value

  if (!seletorCorAberto.value) return

  await nextTick()
  reposicionarPopovers()

  if (colorPicker) return

  const root = colorPickerRoot.value
  if (!root) return

  const { default: iro } = await import('@jaames/iro')
  colorPicker = new iro.ColorPicker(root, {
    width: 220,
    color: cor.value,
    borderWidth: 1,
    borderColor: '#e2e8f0',
    layout: [
      { component: iro.ui.Box },
      { component: iro.ui.Slider, options: { sliderType: 'hue' } }
    ]
  })
  colorPickerChange = (corSelecionada) => {
    cor.value = corSelecionada.hexString
  }
  colorPicker.on('color:change', colorPickerChange)
  await nextTick()
  reposicionarPopovers()
}

async function abrirSeletorEmoji() {
  seletorCorAberto.value = false
  seletorEmojiAberto.value = !seletorEmojiAberto.value

  if (!seletorEmojiAberto.value) return

  await nextTick()
  reposicionarPopovers()

  await import('emoji-picker-element')
  await nextTick()

  const root = emojiPickerRoot.value
  if (!root || root.childElementCount) return

  emojiPickerElement = document.createElement('emoji-picker')
  emojiPickerElement.setAttribute('locale', 'pt_BR')
  emojiPickerElement.addEventListener('emoji-click', (evento: Event) => {
    const emoji = (evento as CustomEvent<{ unicode?: string }>).detail?.unicode
    if (emoji) {
      icone.value = emoji
      seletorEmojiAberto.value = false
    }
  })
  root.appendChild(emojiPickerElement)
}

function aplicarHexManual() {
  const valor = hexManual.value.trim()
  if (/^#[0-9a-f]{6}$/i.test(valor)) {
    cor.value = valor
  }
}

function alternarDia(dia: number) {
  if (horario.value.dias.includes(dia)) {
    horario.value.dias = horario.value.dias.filter(item => item !== dia)
    return
  }

  horario.value.dias = [...horario.value.dias, dia]
}

function salvar() {
  const nomeLimpo = nome.value.trim()

  if (!nomeLimpo) {
    erroValidacao.value = 'Informe o nome da disciplina.'
    campoNome.value?.focus()
    return
  }

  if (!horarioValido.value) {
    if (horario.value.dias.length === 0) {
      erroHorario.value = 'Selecione pelo menos um dia da semana.'
    }
    else if (!horario.value.horaInicio || !horario.value.horaFim) {
      erroHorario.value = 'Informe o horário de início e fim.'
    }
    else if (!horario.value.dataInicio || !horario.value.dataFim) {
      erroHorario.value = 'Informe a data inicial e final.'
    }
    else if (horario.value.horaFim <= horario.value.horaInicio) {
      erroHorario.value = 'O horário final deve ser depois do horário inicial.'
    }
    else {
      erroHorario.value = 'A data final deve ser depois da data inicial.'
    }
    return
  }

  erroValidacao.value = ''
  erroHorario.value = ''
  emit('salvar', {
    nome: nomeLimpo,
    descricao: '',
    cor: cor.value,
    icone: icone.value,
    horario: {
      dias: [...horario.value.dias],
      horaInicio: horario.value.horaInicio,
      horaFim: horario.value.horaFim,
      dataInicio: horario.value.dataInicio,
      dataFim: horario.value.dataFim
    }
  })
}

onMounted(() => {
  overflowAnterior = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  window.addEventListener('keydown', aoPressionarTecla)
  window.addEventListener('pointerdown', aoClicarFora)
  window.addEventListener('resize', reposicionarPopovers)
  window.addEventListener('scroll', reposicionarPopovers, true)
  nextTick(() => campoNome.value?.focus())
})

onBeforeUnmount(() => {
  document.body.style.overflow = overflowAnterior
  window.removeEventListener('keydown', aoPressionarTecla)
  window.removeEventListener('pointerdown', aoClicarFora)
  window.removeEventListener('resize', reposicionarPopovers)
  window.removeEventListener('scroll', reposicionarPopovers, true)

  if (colorPicker && colorPickerChange) {
    colorPicker.off('color:change', colorPickerChange)
  }
})
</script>

<template>
  <div class="modal-overlay" @click.self="fechar">
    <section
      class="modal-criacao"
      role="dialog"
      aria-modal="true"
      aria-labelledby="titulo-criar-disciplina"
    >
      <header class="modal-header">
        <div>
          <h2 id="titulo-criar-disciplina">Nova disciplina</h2>
          <p>Defina os detalhes para organizar suas aulas.</p>
        </div>
        <button
          type="button"
          class="botao-fechar"
          aria-label="Fechar"
          :disabled="salvando"
          @click="fechar"
        >
          ×
        </button>
      </header>

      <form @submit.prevent="salvar">
        <label class="campo-label" for="nome-disciplina">
          Nome <span aria-hidden="true">*</span>
        </label>
        <input
          id="nome-disciplina"
          ref="campoNome"
          v-model="nome"
          class="campo-texto"
          type="text"
          maxlength="80"
          placeholder="Ex.: Matemática"
          autocomplete="off"
          required
        >
        <p v-if="erroValidacao" class="erro-campo" role="alert">
          {{ erroValidacao }}
        </p>

        <div class="grupo-campo">
          <span class="campo-label">Personalização</span>
          <div class="botoes-seletor">
            <div class="controle-seletor">
              <button
                ref="corTrigger"
                type="button"
                class="botao-seletor"
                :aria-expanded="seletorCorAberto"
                aria-label="Selecionar cor"
                title="Selecionar cor"
                @click="abrirSeletorCor"
              >
                <Palette :size="14" :stroke-width="1.8" aria-hidden="true" />
                <span class="indicador-cor" :style="{ background: cor }" aria-hidden="true" />
              </button>
            </div>

            <div class="controle-seletor">
              <button
                ref="emojiTrigger"
                type="button"
                class="botao-seletor"
                :aria-expanded="seletorEmojiAberto"
                aria-label="Selecionar ícone"
                title="Selecionar ícone"
                @click="abrirSeletorEmoji"
              >
                <Sticker :size="14" :stroke-width="1.8" aria-hidden="true" />
                <span class="indicador-icone" aria-hidden="true">{{ icone }}</span>
              </button>
            </div>
          </div>
        </div>

        <div class="grupo-campo">
          <div class="cabecalho-horario">
            <div>
              <span class="campo-label">Horários recorrentes</span>
              <p class="descricao-horario">Configure quando esta disciplina acontece.</p>
            </div>
          </div>

          <div class="dias-semana" aria-label="Dias da semana">
            <button
              v-for="dia in diasSemana"
              :key="dia.numero"
              type="button"
              class="botao-dia"
              :class="{ ativo: horario.dias.includes(dia.numero) }"
              @click="alternarDia(dia.numero)"
            >
              {{ dia.nome }}
            </button>
          </div>

          <div class="linha-horario">
            <div class="campo-horario">
              <label for="hora-inicio-disciplina">Início</label>
              <input id="hora-inicio-disciplina" v-model="horario.horaInicio" type="time">
            </div>

            <div class="campo-horario">
              <label for="hora-fim-disciplina">Fim</label>
              <input id="hora-fim-disciplina" v-model="horario.horaFim" type="time">
            </div>
          </div>

          <div class="linha-horario">
            <div class="campo-horario">
              <label for="data-inicial-disciplina">Data inicial</label>
              <input id="data-inicial-disciplina" v-model="horario.dataInicio" type="date">
            </div>

            <div class="campo-horario">
              <label for="data-final-disciplina">Data final</label>
              <input id="data-final-disciplina" v-model="horario.dataFim" type="date">
            </div>
          </div>

          <p v-if="erroHorario" class="erro-campo" role="alert">
            {{ erroHorario }}
          </p>

          <div class="botoes-horario">
            <button type="button" class="botao-cancelar botao-horario-cancelar" :disabled="salvando" @click="fechar">
              Cancelar
            </button>
            <button
              type="button"
              class="botao-salvar botao-horario-salvar"
              :disabled="salvando || !nome.trim()"
              @click="salvar"
            >
              {{ salvando ? 'Salvando...' : 'Salvar disciplina' }}
            </button>
          </div>
        </div>

        <p v-if="erro" class="erro-salvamento" role="alert">
          {{ erro }}
        </p>
      </form>
    </section>

    <Teleport to="body">
      <div
        v-show="seletorCorAberto"
        ref="popoverCorRoot"
        class="popover-cor"
        :style="popoverCorPosition"
        @click.stop
      >
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
            @click="cor = opcao"
          >
            <span v-if="cor.toLowerCase() === opcao" aria-hidden="true">✓</span>
          </button>
        </div>

        <div ref="colorPickerRoot" class="seletor-espectro" />

        <div class="linha-hex">
          <span class="previa-hex" :style="{ background: cor }" />
          <label for="cor-hex">HEX</label>
          <input
            id="cor-hex"
            v-model="hexManual"
            type="text"
            maxlength="7"
            aria-label="Código hexadecimal da cor"
            @input="aplicarHexManual"
            @blur="aplicarHexManual"
          >
        </div>

        <button
          type="button"
          class="confirmar-seletor"
          @click="seletorCorAberto = false"
        >
          Concluir
        </button>
      </div>

      <div
        v-show="seletorEmojiAberto"
        ref="popoverEmojiRoot"
        class="popover-emoji"
        :style="popoverEmojiPosition"
        @click.stop
      >
        <div ref="emojiPickerRoot" class="seletor-emoji" />
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgb(15 23 42 / 55%);
  animation: aparecer 160ms ease-out;
}

.modal-criacao {
  width: min(100%, 560px);
  max-height: min(92vh, 760px);
  overflow-y: auto;
  padding: 24px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 24px 70px rgb(15 23 42 / 24%);
  animation: subir 180ms ease-out;
}

.modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
}

.modal-header h2 {
  color: #0f172a;
  font-size: 21px;
  font-weight: 700;
}

.modal-header p {
  margin-top: 4px;
  color: #64748b;
  font-size: 14px;
}

.botao-fechar {
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #64748b;
  font-size: 26px;
  line-height: 1;
  cursor: pointer;
}

.botao-fechar:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.campo-label {
  display: block;
  margin-bottom: 8px;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}

.campo-label > span:first-child:not(.opcional) {
  color: #dc2626;
}

.cabecalho-horario {
  margin-bottom: 10px;
}

.descricao-horario {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.dias-semana {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.botao-dia {
  min-width: 48px;
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: white;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 140ms ease;
}

.botao-dia:hover {
  border-color: #94a3b8;
}

.botao-dia.ativo {
  border-color: #2563eb;
  background: #eff6ff;
  color: #1d4ed8;
}

.linha-horario {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.campo-horario {
  min-width: 0;
}

.campo-horario label {
  display: block;
  margin-bottom: 6px;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
}

.campo-horario input {
  width: 100%;
  min-height: 40px;
  padding: 9px 10px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: white;
  color: #0f172a;
}

.campo-horario input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgb(37 99 235 / 12%);
  outline: none;
}

.botoes-horario {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

.botao-horario-cancelar {
  min-height: 34px;
  padding: 0 12px;
}

.botao-horario-salvar {
  min-height: 34px;
  padding: 0 12px;
}

.campo-texto {
  width: 100%;
  min-height: 42px;
  padding: 10px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: white;
  color: #0f172a;
  outline: none;
  transition: border-color 150ms ease, box-shadow 150ms ease;
}

.campo-texto:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgb(37 99 235 / 14%);
}

.grupo-campo {
  min-width: 0;
  margin-top: 20px;
  border: 0;
  padding: 0;
}

.grupo-campo legend {
  padding: 0;
}

.botoes-seletor {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.controle-seletor {
  position: relative;
}

.botao-seletor {
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
  transition: border-color 140ms ease, background-color 140ms ease;
}

.botao-seletor:hover,
.botao-seletor[aria-expanded="true"] {
  border-color: #cbd5e1;
  background: #f8fafc;
}

.botao-seletor:focus-visible {
  outline: 2px solid #2563eb;
  outline-offset: 2px;
}

.indicador-cor,
.indicador-icone {
  position: absolute;
  right: 1px;
  bottom: 1px;
  width: 7px;
  height: 7px;
  border: 1px solid #fff;
  border-radius: 50%;
}

.indicador-icone {
  display: grid;
  place-items: center;
  width: 9px;
  height: 9px;
  overflow: hidden;
  background: #fff;
  font-size: 7px;
  line-height: 1;
}

.popover-cor,
.popover-emoji {
  position: fixed;
  z-index: 1000;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 12px 28px rgb(15 23 42 / 18%);
  max-height: calc(100vh - 24px);
  overflow-y: auto;
}

.popover-cor {
  width: min(276px, calc(100vw - 48px));
  padding: 12px;
}

.cores-rapidas {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 2px 4px 12px;
  border-bottom: 1px solid #e2e8f0;
}

.amostra-cor {
  display: grid;
  width: 22px;
  height: 22px;
  place-items: center;
  border: 1px solid rgb(15 23 42 / 10%);
  border-radius: 50%;
  color: white;
  font-size: 12px;
  cursor: pointer;
  transition: transform 140ms ease, box-shadow 140ms ease;
}

.amostra-cor:hover {
  transform: scale(1.1);
}

.amostra-cor.selecionada {
  box-shadow: 0 0 0 2px white, 0 0 0 3px #0f172a;
}

.seletor-espectro {
  display: flex;
  justify-content: center;
  padding: 10px 0 4px;
}

.linha-hex {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.previa-hex {
  width: 22px;
  height: 22px;
  flex: 0 0 22px;
  border: 1px solid #cbd5e1;
  border-radius: 50%;
}

.linha-hex label {
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
}

.linha-hex input {
  min-width: 0;
  width: 100%;
  height: 32px;
  padding: 0 8px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  color: #334155;
  font-family: monospace;
  font-size: 12px;
}

.linha-hex input:focus {
  border-color: #2563eb;
  outline: 2px solid rgb(37 99 235 / 14%);
}

.confirmar-seletor {
  width: 100%;
  min-height: 34px;
  margin-top: 10px;
  border: 0;
  border-radius: 6px;
  background: #0f172a;
  color: white;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.confirmar-seletor:hover {
  background: #1e293b;
}

.popover-emoji {
  width: min(360px, calc(100vw - 40px));
  overflow: hidden;
}

.seletor-emoji :deep(emoji-picker) {
  display: block;
  width: 100%;
  height: 360px;
  --border-size: 0;
  --border-radius: 8px;
  --num-columns: 8;
  --emoji-size: 1.45rem;
  --emoji-padding: 0.45rem;
  --background: #fff;
  --indicator-color: #2563eb;
  --button-hover-background: #eff6ff;
}

.erro-campo,
.erro-salvamento {
  margin-top: 6px;
  color: #b91c1c;
  font-size: 13px;
}

.erro-salvamento {
  margin-top: 16px;
}

.botao-fechar:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.botao-cancelar {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: white;
  color: #334155;
  font-weight: 600;
  cursor: pointer;
}

.botao-cancelar:hover:not(:disabled) {
  background: #f8fafc;
}

.botao-salvar {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid #2563eb;
  border-radius: 6px;
  background: #2563eb;
  color: white;
  font-weight: 600;
  cursor: pointer;
}

.botao-salvar:hover:not(:disabled) {
  background: #1d4ed8;
}

.botao-salvar:disabled,
.botao-cancelar:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

@keyframes aparecer {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes subir {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 480px) {
  .modal-overlay {
    padding: 10px;
  }

  .modal-criacao {
    padding: 18px;
  }

  .popover-emoji {
    width: min(340px, calc(100vw - 36px));
  }

  .botoes-horario button {
    flex: 1;
    padding: 0 10px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .modal-overlay,
  .modal-criacao,
  .campo-texto,
  .amostra-cor,
  .botao-seletor,
  .amostra-cor,
  .confirmar-seletor,
  .botoes-horario button {
    animation: none;
    transition: none;
  }
}
</style>