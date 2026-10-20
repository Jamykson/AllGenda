<script setup lang="ts">
import { ChevronDown, ChevronLeft, ChevronRight, ChevronUp, ImagePlus, Maximize2, Minimize2 } from '@lucide/vue'
import type { ImagemAula, ModoPainel } from '~/types/anotacao'

// Galeria das imagens avulsas da aula (em geral, fotos do quadro), no topo da página da aula.
// A ordem é manual e vale para todos: arrastar uma miniatura, ou usar os botões ◀ ▶, salva a nova ordem.
// O tamanho das miniaturas é uma preferência de quem está vendo e fica guardado neste navegador.

const CHAVE_TAMANHO = 'allgenda-tamanho-miniaturas'
const TAMANHO_MINIMO = 80
const TAMANHO_MAXIMO = 320
const TAMANHO_PADRAO = 160

const props = defineProps<{
  imagens: ImagemAula[]
  // null enquanto a aula é recorrente e ainda não existe no backend
  aulaId: string | null
  obterAulaId: () => Promise<string>
  modo: ModoPainel
  carregando: boolean
}>()

const emit = defineEmits<{
  'update:imagens': [imagens: ImagemAula[]]
  'update:modo': [modo: ModoPainel]
  // perguntar ao assistente sobre a imagem
  perguntar: [imagem: ImagemAula]
}>()

const { adicionarImagemAula, reordenarImagensDaAula } = useAnotacoes()

const tamanho = ref(TAMANHO_PADRAO)
const envio = ref<{ atual: number, total: number } | null>(null)
const erro = ref('')
const indiceAberto = ref<number | null>(null)
const arrastandoId = ref<string | null>(null)
const alvo = ref<{ id: string, depois: boolean } | null>(null)
let filaOrdem: Promise<void> = Promise.resolve()

const titulo = computed(() => `Imagens da aula (${props.imagens.length})`)

onMounted(() => {
  // o localStorage pode não estar disponível (aba anônima, dados bloqueados): fica o tamanho padrão
  try {
    const salvo = Number(localStorage.getItem(CHAVE_TAMANHO))
    if (salvo >= TAMANHO_MINIMO && salvo <= TAMANHO_MAXIMO) tamanho.value = salvo
  }
  catch {}
})

watch(tamanho, (valor) => {
  try {
    localStorage.setItem(CHAVE_TAMANHO, String(valor))
  }
  catch {}
})

// confere tipo e tamanho antes de enviar; cada arquivo vira uma imagem sem legenda, no fim da galeria
async function adicionarFotos(event: Event) {
  const input = event.target as HTMLInputElement
  const arquivos = Array.from(input.files || [])
  input.value = ''
  if (!arquivos.length) return

  erro.value = ''
  const recusados: string[] = []
  const validos = arquivos.filter((arquivo) => {
    if (!TIPOS_DE_IMAGEM.includes(arquivo.type)) {
      recusados.push(`${arquivo.name} não é PNG, JPEG ou WebP`)
      return false
    }
    if (arquivo.size > TAMANHO_MAXIMO_IMAGEM) {
      recusados.push(`${arquivo.name} passa de 5 MB`)
      return false
    }
    return true
  })

  if (props.modo === 'recolhido') emit('update:modo', 'normal')

  let lista = props.imagens
  for (const [posicao, arquivo] of validos.entries()) {
    envio.value = { atual: posicao + 1, total: validos.length }
    try {
      const aulaId = await props.obterAulaId()
      const nova = await adicionarImagemAula(aulaId, arquivo, '')
      lista = [...lista, nova]
      emit('update:imagens', lista)
    }
    catch (falha) {
      console.error('[anotacao] erro ao enviar foto:', falha)
      recusados.push(`${arquivo.name} (${mensagemDeErro(falha, 'falha no envio')})`)
    }
  }
  envio.value = null

  if (recusados.length) {
    erro.value = `Imagens não enviadas: ${recusados.join('; ')}.`
  }
}

// a ordem muda na tela na hora; se o backend recusar, volta a anterior
function salvarOrdem(novaOrdem: ImagemAula[]) {
  const anterior = props.imagens
  emit('update:imagens', novaOrdem)
  erro.value = ''

  filaOrdem = filaOrdem.then(async () => {
    if (!props.aulaId) return
    try {
      emit('update:imagens', await reordenarImagensDaAula(props.aulaId, novaOrdem.map(imagem => imagem.id)))
    }
    catch (falha) {
      console.error('[anotacao] erro ao reordenar imagens:', falha)
      erro.value = mensagemDeErro(falha, 'Não foi possível salvar a nova ordem das imagens.')
      emit('update:imagens', anterior)
    }
  })
}

function mover(indice: number, destino: number) {
  if (destino < 0 || destino >= props.imagens.length || destino === indice) return
  const lista = [...props.imagens]
  const [imagem] = lista.splice(indice, 1)
  lista.splice(destino, 0, imagem!)
  salvarOrdem(lista)
}

function aoComecarArrasto(evento: DragEvent, imagem: ImagemAula) {
  arrastandoId.value = imagem.id
  if (evento.dataTransfer) {
    evento.dataTransfer.effectAllowed = 'move'
    evento.dataTransfer.setData('text/plain', imagem.id)
  }
}

// a miniatura sob o ponteiro indica se a imagem arrastada vai antes ou depois dela
function aoArrastarSobre(evento: DragEvent, imagem: ImagemAula) {
  if (!arrastandoId.value) return
  evento.preventDefault()
  if (evento.dataTransfer) evento.dataTransfer.dropEffect = 'move'
  const caixa = (evento.currentTarget as HTMLElement).getBoundingClientRect()
  alvo.value = { id: imagem.id, depois: evento.clientX > caixa.left + caixa.width / 2 }
}

function aoSoltar(evento: DragEvent) {
  evento.preventDefault()
  const origem = props.imagens.findIndex(imagem => imagem.id === arrastandoId.value)
  const destinoAtual = alvo.value
  aoTerminarArrasto()
  if (origem < 0 || !destinoAtual || destinoAtual.id === props.imagens[origem]?.id) return

  const lista = [...props.imagens]
  const [imagem] = lista.splice(origem, 1)
  const indiceAlvo = lista.findIndex(item => item.id === destinoAtual.id)
  lista.splice(indiceAlvo + (destinoAtual.depois ? 1 : 0), 0, imagem!)
  salvarOrdem(lista)
}

function aoTerminarArrasto() {
  arrastandoId.value = null
  alvo.value = null
}

function aoSalvarImagem(imagem: ImagemAula) {
  emit('update:imagens', props.imagens.map(item => (item.id === imagem.id ? imagem : item)))
}

function aoExcluirImagem(imagemId: string) {
  const restantes = props.imagens.filter(item => item.id !== imagemId)
  emit('update:imagens', restantes)
  if (!restantes.length) indiceAberto.value = null
  else if (indiceAberto.value !== null) indiceAberto.value = Math.min(indiceAberto.value, restantes.length - 1)
}

// o visualizador cobre a tela toda, então fecha para mostrar o assistente
function perguntarSobre(imagem: ImagemAula) {
  indiceAberto.value = null
  emit('perguntar', imagem)
}
</script>

<template>
  <section class="galeria" :class="`modo-${modo}`" aria-labelledby="titulo-galeria">
    <header class="barra-galeria">
      <button
        type="button"
        class="alternar"
        :aria-expanded="modo !== 'recolhido'"
        aria-controls="conteudo-galeria"
        :title="modo === 'recolhido' ? 'Mostrar imagens' : 'Recolher imagens'"
        @click="emit('update:modo', modo === 'recolhido' ? 'normal' : 'recolhido')"
      >
        <ChevronDown v-if="modo === 'recolhido'" :size="16" :stroke-width="2" aria-hidden="true" />
        <ChevronUp v-else :size="16" :stroke-width="2" aria-hidden="true" />
        <h2 id="titulo-galeria">{{ titulo }}</h2>
      </button>

      <div class="controles">
        <label v-if="modo !== 'recolhido' && imagens.length" class="controle-tamanho">
          <span>Tamanho</span>
          <input
            v-model.number="tamanho"
            type="range"
            :min="TAMANHO_MINIMO"
            :max="TAMANHO_MAXIMO"
            step="10"
            aria-label="Tamanho das miniaturas"
          />
        </label>

        <label class="botao-galeria adicionar" :class="{ desabilitado: envio }" title="PNG, JPEG ou WebP, até 5 MB cada">
          <input
            type="file"
            accept="image/png,image/jpeg,image/webp"
            multiple
            aria-label="Adicionar fotos"
            :disabled="Boolean(envio)"
            @change="adicionarFotos"
          />
          <ImagePlus :size="15" :stroke-width="1.8" aria-hidden="true" />
          {{ envio ? `Enviando ${envio.atual} de ${envio.total}...` : 'Adicionar fotos' }}
        </label>

        <button
          type="button"
          class="botao-galeria icone"
          :title="modo === 'maximizado' ? 'Restaurar tamanho' : 'Ocupar a tela toda'"
          :aria-label="modo === 'maximizado' ? 'Restaurar tamanho da galeria' : 'Maximizar galeria'"
          @click="emit('update:modo', modo === 'maximizado' ? 'normal' : 'maximizado')"
        >
          <Minimize2 v-if="modo === 'maximizado'" :size="15" :stroke-width="1.8" aria-hidden="true" />
          <Maximize2 v-else :size="15" :stroke-width="1.8" aria-hidden="true" />
        </button>
      </div>
    </header>

    <p v-if="erro" class="mensagem-erro" role="alert">{{ erro }}</p>

    <div
      v-show="modo !== 'recolhido'"
      id="conteudo-galeria"
      class="conteudo-galeria"
    >
      <p v-if="carregando" class="estado" role="status">Carregando imagens...</p>
      <p v-else-if="!imagens.length" class="estado">
        Nenhuma imagem nesta aula ainda. Use "Adicionar fotos" para guardar fotos do quadro.
      </p>

      <ol
        v-else
        class="grade"
        :style="{ '--tamanho-miniatura': `${tamanho}px` }"
        aria-label="Imagens da aula, na ordem da galeria"
      >
        <li
          v-for="(imagem, indice) in imagens"
          :key="imagem.id"
          class="miniatura"
          :class="{
            arrastando: imagem.id === arrastandoId,
            'alvo-antes': alvo?.id === imagem.id && !alvo.depois,
            'alvo-depois': alvo?.id === imagem.id && alvo.depois
          }"
          draggable="true"
          @dragstart="aoComecarArrasto($event, imagem)"
          @dragover="aoArrastarSobre($event, imagem)"
          @drop="aoSoltar"
          @dragend="aoTerminarArrasto"
        >
          <button
            type="button"
            class="abrir"
            :aria-label="`Abrir imagem ${indice + 1}${imagem.legenda ? `: ${imagem.legenda}` : ''}`"
            @click="indiceAberto = indice"
          >
            <ImagemDaAnotacao :imagem="{ url: imagem.url, nome: imagem.legenda || imagem.nome }" />
          </button>
          <p v-if="imagem.legenda" class="legenda" :title="imagem.legenda">{{ imagem.legenda }}</p>
          <div class="mover">
            <button
              type="button"
              :aria-label="`Mover imagem ${indice + 1} para antes`"
              title="Mover para antes"
              :disabled="indice === 0"
              @click="mover(indice, indice - 1)"
            >
              <ChevronLeft :size="14" :stroke-width="2" aria-hidden="true" />
            </button>
            <button
              type="button"
              :aria-label="`Mover imagem ${indice + 1} para depois`"
              title="Mover para depois"
              :disabled="indice === imagens.length - 1"
              @click="mover(indice, indice + 1)"
            >
              <ChevronRight :size="14" :stroke-width="2" aria-hidden="true" />
            </button>
          </div>
        </li>
      </ol>
    </div>

    <VisualizadorImagem
      v-if="indiceAberto !== null && imagens.length"
      v-model:indice="indiceAberto"
      :imagens="imagens"
      @fechar="indiceAberto = null"
      @salva="aoSalvarImagem"
      @excluida="aoExcluirImagem"
      @perguntar="perguntarSobre"
    />
  </section>
</template>

<style scoped>
.galeria {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
  background: #fff;
}

.barra-galeria {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 12px;
  padding: 8px 16px;
  border-bottom: 1px solid #e2e8f0;
}

.alternar {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 6px 4px 2px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #334155;
  cursor: pointer;
}

.alternar:hover {
  background: #f1f5f9;
}

.alternar h2 {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.controles {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.controle-tamanho {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 12px;
}

.controle-tamanho input {
  width: 120px;
  accent-color: #2563eb;
}

.botao-galeria {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid #dfe3e8;
  border-radius: 7px;
  background: #fff;
  color: #344054;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

.botao-galeria:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
}

.botao-galeria.icone {
  width: 32px;
  justify-content: center;
  padding: 0;
}

.adicionar {
  border-color: #bfdbfe;
  color: #1d4ed8;
}

.adicionar input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.adicionar.desabilitado {
  opacity: 0.6;
  cursor: progress;
}

.mensagem-erro {
  margin: 0;
  padding: 8px 16px 0;
  color: #b91c1c;
  font-size: 13px;
}

.conteudo-galeria {
  flex: 1;
  min-height: 0;
  padding: 14px 16px;
  overflow-y: auto;
  background: #f8fafc;
}

.estado {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.grade {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(var(--tamanho-miniatura), 1fr));
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.miniatura {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 6px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  cursor: grab;
}

.miniatura.arrastando {
  opacity: 0.4;
}

.miniatura.alvo-antes {
  box-shadow: -4px 0 0 #2563eb;
}

.miniatura.alvo-depois {
  box-shadow: 4px 0 0 #2563eb;
}

.abrir {
  display: block;
  width: 100%;
  aspect-ratio: 4 / 3;
  padding: 0;
  overflow: hidden;
  border: 0;
  border-radius: 7px;
  background: #e2e8f0;
  cursor: zoom-in;
}

.abrir :deep(img),
.abrir :deep(span) {
  width: 100%;
  height: 100%;
  min-height: 0;
  object-fit: cover;
  pointer-events: none;
}

.legenda {
  display: -webkit-box;
  overflow: hidden;
  margin: 0;
  padding: 0 2px;
  color: #334155;
  font-size: 12px;
  line-height: 1.4;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.mover {
  position: absolute;
  top: 10px;
  right: 10px;
  display: flex;
  gap: 2px;
  padding: 2px;
  border-radius: 7px;
  background: rgb(255 255 255 / 90%);
  opacity: 0;
  transition: opacity 120ms ease;
}

.miniatura:hover .mover,
.miniatura:focus-within .mover {
  opacity: 1;
}

@media (hover: none) {
  .mover {
    opacity: 1;
  }
}

.mover button {
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: #334155;
  cursor: pointer;
}

.mover button:hover:not(:disabled) {
  background: #e2e8f0;
}

.mover button:disabled {
  opacity: 0.3;
  cursor: default;
}

.alternar:focus-visible,
.botao-galeria:focus-visible,
.adicionar:focus-within,
.abrir:focus-visible,
.mover button:focus-visible,
.controle-tamanho input:focus-visible {
  outline: 2px solid rgb(37 99 235 / 55%);
  outline-offset: 2px;
}
</style>
