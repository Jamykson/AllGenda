<script setup lang="ts">
import { Image as ImageIcon, PencilLine, Tag as TagIcon, Trash2, Video as VideoIcon } from '@lucide/vue'

interface Aula {
  id: string
  data: string
  horario: string
  topico?: string | null
  recorrente?: boolean
}

interface AnotacaoAula {
  id: string
  texto: string
  imagens: string[]
  videos: string[]
  tags: string[]
}

const props = defineProps<{
  aula: Aula | null
}>()

const emit = defineEmits<{
  fechar: []
}>()

const CHAVE_ANOTACOES = 'allgenda-anotacoes-aulas'
const CHAVE_TAGS = 'allgenda-tags-disponiveis'

const anotacoes = ref<AnotacaoAula[]>([])
const texto = ref('')
const imagens = ref<string[]>([])
const videos = ref<string[]>([])
const tagsSelecionadas = ref<string[]>([])
const novaTag = ref('')
const mostrarEditorTags = ref(false)
const anotacaoEditandoId = ref<string | null>(null)
const campoAnotacao = ref<HTMLTextAreaElement | null>(null)
const tagButtonRef = ref<HTMLButtonElement | null>(null)
const tagPopoverRef = ref<HTMLDivElement | null>(null)
const tagPopoverPosition = ref<Record<string, string>>({ visibility: 'hidden' })
const tagsDisponiveis = ref<string[]>([
  'Prova',
  'Resumo',
  'Leitura',
  'Exercício',
  'Dúvida',
  'Trabalho',
  'Revisão',
  'Importante'
])

function gerarId() {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }

  return `anotacao-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function criarAnotacaoVazia(): AnotacaoAula {
  return {
    id: gerarId(),
    texto: '',
    imagens: [],
    videos: [],
    tags: []
  }
}

function resetarEditor() {
  texto.value = ''
  imagens.value = []
  videos.value = []
  tagsSelecionadas.value = []
  anotacaoEditandoId.value = null
}

function normalizarAnotacao(item: Partial<AnotacaoAula> | null | undefined): AnotacaoAula {
  return {
    id: item?.id || gerarId(),
    texto: item?.texto || '',
    imagens: Array.isArray(item?.imagens) ? item.imagens : [],
    videos: Array.isArray(item?.videos) ? item.videos : [],
    tags: Array.isArray(item?.tags) ? item.tags : []
  }
}

function carregarTagsGlobais() {
  const salvo = localStorage.getItem(CHAVE_TAGS)
  if (!salvo) {
    localStorage.setItem(CHAVE_TAGS, JSON.stringify(tagsDisponiveis.value))
    return
  }

  try {
    const dados = JSON.parse(salvo) as string[]
    if (Array.isArray(dados) && dados.length) {
      tagsDisponiveis.value = dados
    }
  }
  catch {
    tagsDisponiveis.value = [...tagsDisponiveis.value]
  }
}

function carregarAnotacoes() {
  if (!props.aula) {
    anotacoes.value = []
    resetarEditor()
    return
  }

  const salvo = localStorage.getItem(CHAVE_ANOTACOES)
  if (!salvo) {
    anotacoes.value = []
    return
  }

  try {
    const dados = JSON.parse(salvo) as Record<string, any>
    const aulaSalva = dados[props.aula.id]

    if (!aulaSalva) {
      anotacoes.value = []
      return
    }

    if (Array.isArray(aulaSalva.anotacoes)) {
      anotacoes.value = aulaSalva.anotacoes.map(normalizarAnotacao)
      return
    }

    anotacoes.value = [normalizarAnotacao({
      id: gerarId(),
      texto: aulaSalva.texto || '',
      imagens: aulaSalva.imagens || [],
      videos: aulaSalva.videos || [],
      tags: aulaSalva.tags || []
    })]
  }
  catch {
    anotacoes.value = []
  }
}

function salvarAnotacoes() {
  if (!props.aula) return

  const salvo = localStorage.getItem(CHAVE_ANOTACOES)
  const dados = salvo ? JSON.parse(salvo) : {}

  dados[props.aula.id] = {
    anotacoes: anotacoes.value
  }

  localStorage.setItem(CHAVE_ANOTACOES, JSON.stringify(dados))
}

function confirmarAnotacaoAtual() {
  const textoAtual = texto.value.trim()
  const anotacaoEditando = anotacaoEditandoId.value

  if (!textoAtual && !imagens.value.length && !videos.value.length && !tagsSelecionadas.value.length) {
    return
  }

  if (anotacaoEditando) {
    anotacoes.value = anotacoes.value.map((anotacao) =>
      anotacao.id === anotacaoEditando
        ? {
            ...anotacao,
            texto: textoAtual,
            imagens: [...imagens.value],
            videos: [...videos.value],
            tags: [...tagsSelecionadas.value]
          }
        : anotacao
    )
  }
  else {
    anotacoes.value = [
      ...anotacoes.value,
      {
        id: gerarId(),
        texto: textoAtual,
        imagens: [...imagens.value],
        videos: [...videos.value],
        tags: [...tagsSelecionadas.value]
      }
    ]
  }

  salvarAnotacoes()
  resetarEditor()
}

async function editarAnotacao(anotacao: AnotacaoAula) {
  anotacaoEditandoId.value = anotacao.id
  texto.value = anotacao.texto
  imagens.value = [...anotacao.imagens]
  videos.value = [...anotacao.videos]
  tagsSelecionadas.value = [...anotacao.tags]

  await nextTick()
  campoAnotacao.value?.focus()
}

function excluirAnotacao(id: string) {
  if (!window.confirm('Deseja excluir esta anotação?')) return

  anotacoes.value = anotacoes.value.filter((anotacao) => anotacao.id !== id)

  if (anotacaoEditandoId.value === id) {
    resetarEditor()
  }

  salvarAnotacoes()
}

function cancelarEdicaoAnotacao() {
  resetarEditor()
}

function apagarImagem(indice: number) {
  imagens.value = imagens.value.filter((_, index) => index !== indice)
}

function apagarVideo(indice: number) {
  videos.value = videos.value.filter((_, index) => index !== indice)
}

async function adicionarArquivos(event: Event) {
  const input = event.target as HTMLInputElement
  const arquivos = Array.from(input.files || [])

  if (!arquivos.length) return

  for (const arquivo of arquivos) {
    if (arquivo.type.startsWith('image/')) {
      const leitor = new FileReader()
      leitor.onload = () => {
        if (typeof leitor.result === 'string') {
          imagens.value = [...imagens.value, leitor.result]
        }
      }
      leitor.readAsDataURL(arquivo)
    }

    if (arquivo.type.startsWith('video/')) {
      const url = URL.createObjectURL(arquivo)
      videos.value = [...videos.value, url]
    }
  }

  input.value = ''
}

watch(
  () => props.aula,
  () => {
    resetarEditor()
    carregarAnotacoes()
  },
  { immediate: true }
)

function adicionarTag() {
  const tag = novaTag.value.trim()
  if (!tag) return

  const valor = tag.replace(/\s+/g, ' ')
  const tagExistente = tagsDisponiveis.value.find(item => item.toLowerCase() === valor.toLowerCase())

  if (!tagExistente) {
    tagsDisponiveis.value = [...tagsDisponiveis.value, valor]
    localStorage.setItem(CHAVE_TAGS, JSON.stringify(tagsDisponiveis.value))
  }

  if (!tagsSelecionadas.value.some(item => item.toLowerCase() === valor.toLowerCase())) {
    tagsSelecionadas.value = [...tagsSelecionadas.value, valor]
  }

  novaTag.value = ''
}

function removerTag(tag: string) {
  tagsSelecionadas.value = tagsSelecionadas.value.filter(item => item !== tag)
}

function selecionarTag(tag: string) {
  if (!tagsSelecionadas.value.some(item => item.toLowerCase() === tag.toLowerCase())) {
    tagsSelecionadas.value = [...tagsSelecionadas.value, tag]
  }
}

function atualizarPosicaoPopover() {
  const botao = tagButtonRef.value
  const popover = tagPopoverRef.value
  if (!botao || !popover) return

  const ancora = botao.getBoundingClientRect()
  const margem = 8
  const espacamento = 6
  const largura = Math.min(popover.offsetWidth || 320, window.innerWidth - margem * 2)
  const altura = Math.min(popover.offsetHeight || 280, window.innerHeight - margem * 2)
  const cabeAbaixo = ancora.bottom + espacamento + altura <= window.innerHeight - margem
  const top = cabeAbaixo
    ? ancora.bottom + espacamento
    : Math.max(margem, ancora.top - espacamento - altura)
  const left = Math.max(margem, Math.min(ancora.left, window.innerWidth - largura - margem))

  tagPopoverPosition.value = {
    top: `${top}px`,
    left: `${left}px`,
    visibility: 'visible'
  }
}

function removerListenersPopover() {
  window.removeEventListener('resize', atualizarPosicaoPopover)
  window.removeEventListener('scroll', atualizarPosicaoPopover, true)
  document.removeEventListener('pointerdown', aoClicarForaPopover)
  document.removeEventListener('keydown', aoPressionarTeclaPopover)
}

function fecharPopoverTags() {
  mostrarEditorTags.value = false
  removerListenersPopover()
}

function aoClicarForaPopover(evento: PointerEvent) {
  const alvo = evento.target
  if (
    alvo instanceof Node
    && !tagPopoverRef.value?.contains(alvo)
    && !tagButtonRef.value?.contains(alvo)
  ) {
    fecharPopoverTags()
  }
}

function aoPressionarTeclaPopover(evento: KeyboardEvent) {
  if (evento.key === 'Escape') fecharPopoverTags()
}

async function alternarPopoverTags() {
  if (mostrarEditorTags.value) {
    fecharPopoverTags()
    return
  }

  mostrarEditorTags.value = true
  tagPopoverPosition.value = { visibility: 'hidden' }
  await nextTick()
  atualizarPosicaoPopover()

  window.addEventListener('resize', atualizarPosicaoPopover)
  window.addEventListener('scroll', atualizarPosicaoPopover, true)
  document.addEventListener('pointerdown', aoClicarForaPopover)
  document.addEventListener('keydown', aoPressionarTeclaPopover)
}

function formatarData(data: string) {
  const [ano, mes, dia] = data.split('-').map(Number)
  if (!ano || !mes || !dia) return data

  return new Intl.DateTimeFormat('pt-BR', {
    weekday: 'short',
    day: '2-digit',
    month: 'short',
    year: 'numeric'
  }).format(new Date(ano, mes - 1, dia))
}

onMounted(() => {
  carregarTagsGlobais()
})

onBeforeUnmount(removerListenersPopover)
</script>

<template>
  <div v-if="aula" class="modal-overlay" @click.self="emit('fechar')">
    <section class="modal-aula" role="dialog" aria-modal="true" aria-labelledby="titulo-aula-modal">
      <header class="cabecalho-modal">
        <div>
          <h2 id="titulo-aula-modal">{{ aula.topico || 'Aula' }}</h2>
          <p>{{ formatarData(aula.data) }} · {{ aula.horario }}</p>
        </div>
        <button type="button" class="botao-fechar" aria-label="Fechar aula" @click="emit('fechar')">
          ×
        </button>
      </header>

      <div class="area-anotacoes">
        <div class="anotacoes-header">
          <label class="label-visivel">Anotações</label>
        </div>

        <div v-if="anotacoes.length" class="lista-anotacoes">
          <article v-for="anotacao in anotacoes" :key="anotacao.id" class="anotacao-card">
            <div v-if="anotacao.tags.length" class="anotacao-cabecalho">
              <div class="chips">
                <span v-for="tag in anotacao.tags" :key="`${anotacao.id}-${tag}`" class="chip chip-selecionado">
                  {{ tag }}
                </span>
              </div>
            </div>

            <div class="acoes-anotacao">
              <button
                type="button"
                class="acao-anotacao"
                aria-label="Editar anotação"
                title="Editar anotação"
                @click="editarAnotacao(anotacao)"
              >
                <PencilLine :size="15" :stroke-width="1.8" aria-hidden="true" />
              </button>
              <button
                type="button"
                class="acao-anotacao acao-excluir"
                aria-label="Excluir anotação"
                title="Excluir anotação"
                @click="excluirAnotacao(anotacao.id)"
              >
                <Trash2 :size="15" :stroke-width="1.8" aria-hidden="true" />
              </button>
            </div>

            <p
              v-if="anotacao.texto"
              class="texto-anotacao"
              :class="{ 'texto-sem-tags': !anotacao.tags.length }"
            >
              {{ anotacao.texto }}
            </p>

            <div
              v-if="anotacao.imagens.length || anotacao.videos.length"
              class="previews"
              :class="{ 'previews-sem-tags-sem-texto': !anotacao.tags.length && !anotacao.texto }"
            >
              <div v-for="(imagem, index) in anotacao.imagens" :key="`img-${anotacao.id}-${index}`" class="item-midia imagem-item">
                <img :src="imagem" :alt="`Imagem ${index + 1}`" />
              </div>

              <div v-for="(video, index) in anotacao.videos" :key="`video-${anotacao.id}-${index}`" class="item-midia video-item">
                <video :src="video" controls />
              </div>
            </div>
          </article>
        </div>

        <textarea
          id="anotacoes-aula"
          ref="campoAnotacao"
          v-model="texto"
          :placeholder="anotacaoEditandoId ? 'Edite sua anotação...' : 'Escreva suas anotações, ideias, lembretes e observações...'"
          @keydown.enter.prevent="confirmarAnotacaoAtual"
        />

        <div
          v-if="tagsSelecionadas.length"
          class="chips chips-anotacao-atual"
          role="group"
          aria-label="Tags da anotação atual"
        >
          <span
            v-for="tag in tagsSelecionadas"
            :key="tag"
            class="chip chip-selecionado"
          >
            {{ tag }}
          </span>
        </div>

        <div v-if="anotacaoEditandoId" class="estado-edicao">
          <span>Editando anotação</span>
          <button type="button" @click="cancelarEdicaoAnotacao">Cancelar edição</button>
        </div>

        <div class="secao-midia">
          <div class="acoes-contextuais">
            <div class="upload-row">
              <label class="upload-btn" title="Adicionar imagem">
                <input type="file" accept="image/*" multiple aria-label="Adicionar imagem" @change="adicionarArquivos" />
                <ImageIcon :size="15" :stroke-width="1.8" aria-hidden="true" />
              </label>
              <label class="upload-btn" title="Adicionar vídeo">
                <input type="file" accept="video/*" multiple aria-label="Adicionar vídeo" @change="adicionarArquivos" />
                <VideoIcon :size="15" :stroke-width="1.8" aria-hidden="true" />
              </label>
              <button
                ref="tagButtonRef"
                type="button"
                class="btn-editar-tags"
                :aria-expanded="mostrarEditorTags"
                aria-controls="seletor-tags-aula"
                aria-label="Adicionar tag"
                title="Adicionar tag"
                @click="alternarPopoverTags"
              >
                <TagIcon :size="15" :stroke-width="1.8" aria-hidden="true" />
              </button>
            </div>
          </div>

          <div v-if="imagens.length || videos.length" class="previews">
            <div v-for="(imagem, index) in imagens" :key="`img-${index}`" class="item-midia imagem-item">
              <img :src="imagem" :alt="`Imagem ${index + 1}`" />
              <button type="button" @click="apagarImagem(index)">Remover</button>
            </div>

            <div v-for="(video, index) in videos" :key="`video-${index}`" class="item-midia video-item">
              <video :src="video" controls />
              <button type="button" @click="apagarVideo(index)">Remover</button>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>

  <Teleport to="body">
    <div
      v-if="mostrarEditorTags"
      id="seletor-tags-aula"
      ref="tagPopoverRef"
      class="secao-tags"
      :style="tagPopoverPosition"
      @click.stop
    >
      <div class="input-tag-row">
        <div class="campo-tag">
          <TagIcon :size="14" :stroke-width="1.8" aria-hidden="true" />
          <input
            v-model="novaTag"
            type="text"
            placeholder="Digite uma tag e pressione Enter"
            aria-label="Nova tag"
            @keydown.enter.prevent="adicionarTag"
          />
        </div>
        <button type="button" class="btn-tag" @click="adicionarTag">Adicionar</button>
      </div>

      <div v-if="tagsSelecionadas.length" class="chips chips-selecionadas" aria-label="Tags selecionadas">
        <button
          v-for="tag in tagsSelecionadas"
          :key="tag"
          type="button"
          class="chip chip-selecionado"
          :aria-label="`Remover tag ${tag}`"
          @click="removerTag(tag)"
        >
          {{ tag }} <span aria-hidden="true">×</span>
        </button>
      </div>

      <div v-if="tagsDisponiveis.length" class="opcoes-tags">
        <p class="rotulo-opcoes">Selecione uma opção</p>
        <div class="tags-disponiveis">
          <button
            v-for="tag in tagsDisponiveis"
            :key="tag"
            type="button"
            class="chip"
            :class="{ 'chip-selecionado': tagsSelecionadas.some(item => item.toLowerCase() === tag.toLowerCase()) }"
            :aria-pressed="tagsSelecionadas.some(item => item.toLowerCase() === tag.toLowerCase())"
            @click="selecionarTag(tag)"
          >
            {{ tag }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgb(15 23 42 / 35%);
  padding: 20px;
}

.modal-aula {
  width: min(760px, 100%);
  max-height: min(88vh, 760px);
  overflow-y: auto;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 18px 50px rgb(15 23 42 / 16%);
  padding: 20px 22px 18px;
}

.anotacoes-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.btn-editar-tags {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  min-height: 34px;
  padding: 0;
  border: 1px solid #dfe3e8;
  border-radius: 7px;
  background: #fff;
  color: #344054;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 150ms ease, border-color 150ms ease, color 150ms ease;
}

.btn-editar-tags:hover,
.upload-btn:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
  color: #1f2937;
}

.secao-tags {
  position: fixed;
  z-index: 100;
  box-sizing: border-box;
  width: min(320px, 100%);
  max-height: min(340px, calc(100dvh - 96px));
  overflow-y: auto;
  display: grid;
  gap: 10px;
  padding: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 6px 20px rgb(15 23 42 / 10%);
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chips-selecionadas {
  margin: 0;
}

.chips-anotacao-atual .chip {
  min-height: 24px;
  padding: 2px 7px;
  cursor: default;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 26px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: #f8fafc;
  color: #475467;
  padding: 3px 8px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 120ms ease, border-color 120ms ease;
}

.chip:hover {
  border-color: #d0d5dd;
  background: #f2f4f7;
}

.chip-selecionado {
  background: #eff6ff;
  border-color: #dbeafe;
  color: #1d4ed8;
}

.lista-anotacoes {
  display: grid;
  gap: 10px;
}

.anotacao-card {
  position: relative;
  border: 1px solid #dbeafe;
  border-radius: 12px;
  background: #f8fafc;
  padding: 12px;
  display: grid;
  gap: 10px;
}

.anotacao-cabecalho {
  min-width: 0;
  padding-right: 72px;
}

.acoes-anotacao {
  display: flex;
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 1;
  gap: 4px;
}

.acao-anotacao {
  display: grid;
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  place-items: center;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
}

.acao-anotacao:hover {
  border-color: #e5e7eb;
  background: #f1f5f9;
  color: #2563eb;
}

.acao-excluir {
  color: #64748b;
}

.acao-excluir:hover {
  border-color: #fee2e2;
  background: #fef2f2;
  color: #b91c1c;
}

.acao-anotacao:focus-visible {
  outline: 2px solid rgb(37 99 235 / 55%);
  outline-offset: 2px;
}

.estado-edicao {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #475569;
  font-size: 12px;
}

.estado-edicao button {
  border: 0;
  background: transparent;
  color: #2563eb;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.texto-anotacao {
  margin: 0;
  white-space: pre-wrap;
  color: #0f172a;
  line-height: 1.5;
}

.texto-sem-tags {
  padding-right: 72px;
}

.input-tag-row {
  display: flex;
  gap: 7px;
  align-items: center;
}

.campo-tag {
  display: flex;
  flex: 1;
  min-width: 0;
  height: 34px;
  align-items: center;
  gap: 7px;
  padding: 0 9px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  color: #98a2b3;
}

.campo-tag:focus-within {
  border-color: #93c5fd;
  box-shadow: 0 0 0 2px rgb(37 99 235 / 10%);
}

.campo-tag > svg {
  flex: 0 0 auto;
}

.input-tag-row input {
  flex: 1;
  min-width: 0;
  height: 30px;
  border: 0;
  outline: none;
  padding: 0;
  font: inherit;
  font-size: 13px;
  background: transparent;
  color: #344054;
}

.input-tag-row input::placeholder {
  color: #98a2b3;
  opacity: 1;
}

.btn-tag {
  min-height: 34px;
  padding: 0 9px;
  border: 1px solid #dfe3e8;
  border-radius: 6px;
  background: #fff;
  color: #344054;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 150ms ease, border-color 150ms ease;
}

.btn-tag:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
}

.opcoes-tags {
  display: grid;
  gap: 7px;
}

.rotulo-opcoes {
  margin: 0;
  color: #667085;
  font-size: 12px;
  font-weight: 600;
}

.tags-disponiveis {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.cabecalho-modal {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 18px;
}

.cabecalho-modal h2 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  font-weight: 700;
}

.cabecalho-modal p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.botao-fechar {
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  font-size: 28px;
  line-height: 1;
  cursor: pointer;
}

.botao-fechar:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.area-anotacoes {
  display: grid;
  gap: 12px;
}

.label-visivel {
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}

textarea {
  width: 100%;
  min-height: 180px;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: 12px 14px;
  resize: vertical;
  font: inherit;
  color: #0f172a;
  box-sizing: border-box;
}

textarea:focus {
  outline: 2px solid #bfdbfe;
  outline-offset: 1px;
}

.upload-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.acoes-contextuais {
  position: relative;
  width: 100%;
}

.upload-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  min-height: 34px;
  padding: 0;
  border: 1px solid #dfe3e8;
  border-radius: 7px;
  background: #fff;
  color: #344054;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 150ms ease, border-color 150ms ease, color 150ms ease;
}

.upload-btn input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.upload-btn:focus-within,
.btn-editar-tags:focus-visible,
.btn-tag:focus-visible,
.chip:focus-visible {
  outline: 2px solid rgb(37 99 235 / 55%);
  outline-offset: 2px;
}

.previews {
  display: grid;
  gap: 12px;
  margin-top: 6px;
}

.previews-sem-tags-sem-texto {
  padding-right: 72px;
}

.item-midia {
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
}

.item-midia img,
.item-midia video {
  width: 100%;
  max-height: 220px;
  object-fit: cover;
  border-radius: 8px;
  background: #e2e8f0;
}

.item-midia button {
  justify-self: flex-start;
  border: 0;
  background: transparent;
  color: #b91c1c;
  font-weight: 600;
  cursor: pointer;
}

@media (max-width: 560px) {
  .modal-aula {
    padding: 16px 14px;
  }

  .cabecalho-modal {
    align-items: center;
  }
}
</style>
