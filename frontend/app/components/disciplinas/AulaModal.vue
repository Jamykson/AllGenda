<script setup lang="ts">
interface Aula {
  id: string
  data: string
  horario: string
  topico: string | null
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

  if (!textoAtual && !imagens.value.length && !videos.value.length && !tagsSelecionadas.value.length) {
    return
  }

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

  salvarAnotacoes()
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

        <div v-if="tagsSelecionadas.length" class="chips chips-selecionadas">
          <button
            v-for="tag in tagsSelecionadas"
            :key="tag"
            type="button"
            class="chip chip-selecionado"
            @click="removerTag(tag)"
          >
            {{ tag }} ×
          </button>
        </div>

        <div v-if="mostrarEditorTags" class="secao-tags">
          <div class="input-tag-row">
            <input
              v-model="novaTag"
              type="text"
              placeholder="Digite uma tag e pressione Enter"
              @keydown.enter.prevent="adicionarTag"
            />
            <button type="button" class="btn-tag" @click="adicionarTag">Adicionar</button>
          </div>

          <div v-if="tagsDisponiveis.length" class="tags-disponiveis">
            <button
              v-for="tag in tagsDisponiveis"
              :key="tag"
              type="button"
              class="chip"
              :class="{ 'chip-selecionado': tagsSelecionadas.some(item => item.toLowerCase() === tag.toLowerCase()) }"
              @click="selecionarTag(tag)"
            >
              {{ tag }}
            </button>
          </div>
        </div>

        <div v-if="anotacoes.length" class="lista-anotacoes">
          <article v-for="anotacao in anotacoes" :key="anotacao.id" class="anotacao-card">
            <div v-if="anotacao.tags.length" class="chips">
              <span v-for="tag in anotacao.tags" :key="`${anotacao.id}-${tag}`" class="chip chip-selecionado">
                {{ tag }}
              </span>
            </div>

            <p v-if="anotacao.texto" class="texto-anotacao">{{ anotacao.texto }}</p>

            <div v-if="anotacao.imagens.length || anotacao.videos.length" class="previews">
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
          v-model="texto"
          placeholder="Escreva suas anotações, ideias, lembretes e observações..."
          @keydown.enter.prevent="confirmarAnotacaoAtual"
        />

        <div class="secao-midia">
          <div class="upload-row">
            <label class="upload-btn">
              <input type="file" accept="image/*" multiple @change="adicionarArquivos" />
              + Imagem
            </label>
            <label class="upload-btn">
              <input type="file" accept="video/*" multiple @change="adicionarArquivos" />
              + Vídeo
            </label>
            <button type="button" class="btn-editar-tags" @click="mostrarEditorTags = !mostrarEditorTags">
              Editar tags
            </button>
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
  border: 0;
  border-radius: 10px;
  background: #111827;
  color: #fff;
  font-weight: 700;
  font-size: 13px;
  padding: 8px 12px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgb(15 23 42 / 12%);
}

.secao-tags {
  display: grid;
  gap: 8px;
  padding: 10px 0 0;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chips-selecionadas {
  margin-top: 4px;
}

.chip {
  border: 1px solid #cbd5e1;
  border-radius: 999px;
  background: #f8fafc;
  color: #334155;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.chip-selecionado {
  background: #dbeafe;
  border-color: #93c5fd;
  color: #1d4ed8;
}

.lista-anotacoes {
  display: grid;
  gap: 10px;
}

.anotacao-card {
  border: 1px solid #dbeafe;
  border-radius: 12px;
  background: #f8fafc;
  padding: 12px;
  display: grid;
  gap: 10px;
}

.texto-anotacao {
  margin: 0;
  white-space: pre-wrap;
  color: #0f172a;
  line-height: 1.5;
}

.input-tag-row {
  display: flex;
  gap: 8px;
}

.input-tag-row input {
  flex: 1;
  min-width: 0;
  height: 36px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0 10px;
  font: inherit;
}

.btn-tag {
  height: 36px;
  border: 1px solid #2563eb;
  border-radius: 8px;
  background: #2563eb;
  color: white;
  font-weight: 600;
  padding: 0 12px;
  cursor: pointer;
}

.tags-disponiveis {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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
  gap: 10px;
  flex-wrap: wrap;
}

.upload-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  padding: 0 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  color: #334155;
  font-weight: 600;
  cursor: pointer;
}

.upload-btn input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.previews {
  display: grid;
  gap: 12px;
  margin-top: 6px;
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
