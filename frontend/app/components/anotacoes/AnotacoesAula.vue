<script setup lang="ts">
import { Image as ImageIcon, PencilLine, Tag as TagIcon, Trash2 } from '@lucide/vue'
import type { Anotacao, AulaDaAnotacao, ImagemAnotacao } from '~/types/anotacao'

// Anotações de uma aula: lista, cria, edita, exclui e anexa imagens, tudo pela API.
// Usado pelo AulaModal e pela página /aulas/[id].

interface ArquivoPendente {
  id: string
  arquivo: File
  preview: string // URL temporária para a prévia; liberada ao remover ou salvar
}

const props = defineProps<{
  aula: AulaDaAnotacao
}>()

const emit = defineEmits<{
  alterado: []
  'aula-cadastrada': [aula: AulaDaAnotacao]
}>()

const TAGS_SUGERIDAS = [
  'Prova',
  'Resumo',
  'Leitura',
  'Exercício',
  'Dúvida',
  'Trabalho',
  'Revisão',
  'Importante'
]

const {
  listarPorAula,
  listarTags,
  criar,
  editar,
  acrescentarTags,
  excluir,
  anexarImagem,
  removerImagem,
  cadastrarAulaRecorrente
} = useAnotacoes()
const { usuario } = useAuth()

const aulaAtual = ref<AulaDaAnotacao>({ ...props.aula })
const anotacoes = ref<Anotacao[]>([])
const carregando = ref(false)
const erroLista = ref('')

const texto = ref('')
const tagsSelecionadas = ref<string[]>([]) // tags que serão acrescentadas ao salvar
const imagensRemovidas = ref<string[]>([]) // ids das imagens já salvas que serão removidas ao salvar
const arquivosNovos = ref<ArquivoPendente[]>([])
const anotacaoEditando = ref<Anotacao | null>(null)
const salvando = ref(false)
const erroEditor = ref('')

const novaTag = ref('')
const mostrarEditorTags = ref(false)
const campoAnotacao = ref<HTMLTextAreaElement | null>(null)
const tagButtonRef = ref<HTMLButtonElement | null>(null)
const tagPopoverRef = ref<HTMLDivElement | null>(null)
const tagPopoverPosition = ref<Record<string, string>>({ visibility: 'hidden' })
const tagsDisponiveis = ref<string[]>([...TAGS_SUGERIDAS])

// a API não remove tags de uma anotação: na edição, as tags que ela já tem ficam fixas
const tagsFixas = computed(() => anotacaoEditando.value?.tags ?? [])
const imagensMantidas = computed(() =>
  (anotacaoEditando.value?.imagens ?? []).filter(imagem => !imagensRemovidas.value.includes(imagem.id))
)
const tagsDoEditor = computed(() => [...tagsFixas.value, ...tagsSelecionadas.value])

function mesmaTag(tagA: string, tagB: string) {
  return tagA.toLowerCase() === tagB.toLowerCase()
}

function ehDoUsuario(anotacao: Anotacao) {
  return Boolean(usuario.value) && anotacao.autorId === usuario.value?.id
}

function gerarId() {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }

  return `arquivo-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function liberarArquivosNovos() {
  arquivosNovos.value.forEach(item => URL.revokeObjectURL(item.preview))
  arquivosNovos.value = []
}

function resetarEditor() {
  texto.value = ''
  tagsSelecionadas.value = []
  imagensRemovidas.value = []
  liberarArquivosNovos()
  anotacaoEditando.value = null
  erroEditor.value = ''
}

async function carregarAnotacoes() {
  // uma aula recorrente ainda não existe no backend, então não tem anotações
  if (aulaAtual.value.recorrente) {
    anotacoes.value = []
    return
  }

  carregando.value = true
  erroLista.value = ''

  try {
    anotacoes.value = await listarPorAula(aulaAtual.value.id)
  }
  catch (erro) {
    console.error('[anotacao] erro ao carregar anotações:', erro)
    erroLista.value = mensagemDeErro(erro, 'Não foi possível carregar as anotações desta aula.')
  }
  finally {
    carregando.value = false
  }

  // depois de recarregar, a anotação em edição passa a refletir o que está salvo
  const emEdicao = anotacaoEditando.value
  if (emEdicao) {
    const atualizada = anotacoes.value.find(anotacao => anotacao.id === emEdicao.id)
    if (!atualizada) {
      resetarEditor()
      return
    }
    anotacaoEditando.value = atualizada
    tagsSelecionadas.value = tagsSelecionadas.value.filter(tag => !atualizada.tags.some(fixa => mesmaTag(fixa, tag)))
    imagensRemovidas.value = imagensRemovidas.value.filter(id => atualizada.imagens.some(imagem => imagem.id === id))
  }
}

async function carregarTags() {
  try {
    const tagsDaApi = await listarTags()
    const novas = tagsDaApi.filter(tag => !tagsDisponiveis.value.some(item => mesmaTag(item, tag)))
    tagsDisponiveis.value = [...tagsDisponiveis.value, ...novas]
  }
  catch (erro) {
    // sem a lista da API, ainda dá para digitar tags novas e usar as sugeridas
    console.error('[anotacao] erro ao carregar tags:', erro)
  }
}

// garante que a aula exista no backend antes de salvar a primeira anotação dela
async function idDaAulaNoBackend() {
  if (!aulaAtual.value.recorrente) {
    return aulaAtual.value.id
  }

  const cadastrada = await cadastrarAulaRecorrente(aulaAtual.value)
  aulaAtual.value = { ...aulaAtual.value, ...cadastrada, recorrente: false }
  emit('aula-cadastrada', cadastrada)
  return cadastrada.id
}

// envia as imagens novas uma por vez; devolve a descrição das que falharam
async function enviarImagens(anotacaoId: string) {
  const falhas: string[] = []

  for (const item of arquivosNovos.value) {
    try {
      await anexarImagem(anotacaoId, item.arquivo)
    }
    catch (erro) {
      console.error('[anotacao] erro ao enviar imagem:', erro)
      falhas.push(`${item.arquivo.name} (${mensagemDeErro(erro, 'falha no envio')})`)
    }
  }

  return falhas
}

async function salvar() {
  if (salvando.value) return

  const conteudo = texto.value.trim()
  if (!conteudo) {
    erroEditor.value = 'Escreva o texto da anotação antes de salvar.'
    return
  }

  salvando.value = true
  erroEditor.value = ''
  let falhasDeImagem: string[] = []
  let salvou = false

  try {
    let anotacaoId: string
    const emEdicao = anotacaoEditando.value

    if (emEdicao) {
      if (conteudo !== emEdicao.conteudo) {
        await editar(emEdicao.id, conteudo)
      }
      if (tagsSelecionadas.value.length) {
        await acrescentarTags(emEdicao.id, tagsSelecionadas.value)
      }
      for (const imagem of emEdicao.imagens.filter(item => imagensRemovidas.value.includes(item.id))) {
        await removerImagem(imagem)
      }
      anotacaoId = emEdicao.id
    }
    else {
      const aulaId = await idDaAulaNoBackend()
      const criada = await criar(aulaId, conteudo, tagsSelecionadas.value)
      anotacaoId = criada.id
    }

    emit('alterado')
    falhasDeImagem = await enviarImagens(anotacaoId)
    salvou = true
  }
  catch (erro) {
    console.error('[anotacao] erro ao salvar:', erro)
    erroEditor.value = mensagemDeErro(erro, 'Não foi possível salvar a anotação. Tente novamente.')
  }
  finally {
    if (salvou) {
      resetarEditor()
      if (falhasDeImagem.length) {
        erroEditor.value = `A anotação foi salva, mas estas imagens não foram enviadas: ${falhasDeImagem.join('; ')}.`
      }
    }
    await carregarAnotacoes()
    salvando.value = false
  }
}

async function editarAnotacao(anotacao: Anotacao) {
  resetarEditor()
  anotacaoEditando.value = anotacao
  texto.value = anotacao.conteudo

  await nextTick()
  campoAnotacao.value?.focus()
}

async function excluirAnotacao(anotacao: Anotacao) {
  if (!window.confirm('Deseja excluir esta anotação?')) return

  erroLista.value = ''

  try {
    await excluir(anotacao.id)
    emit('alterado')
    if (anotacaoEditando.value?.id === anotacao.id) {
      resetarEditor()
    }
  }
  catch (erro) {
    console.error('[anotacao] erro ao excluir:', erro)
    erroLista.value = mensagemDeErro(erro, 'Não foi possível excluir a anotação.')
  }

  await carregarAnotacoes()
}

function cancelarEdicaoAnotacao() {
  resetarEditor()
}

function removerImagemSalva(imagem: ImagemAnotacao) {
  imagensRemovidas.value = [...imagensRemovidas.value, imagem.id]
}

function removerArquivoNovo(id: string) {
  const item = arquivosNovos.value.find(arquivo => arquivo.id === id)
  if (item) {
    URL.revokeObjectURL(item.preview)
  }
  arquivosNovos.value = arquivosNovos.value.filter(arquivo => arquivo.id !== id)
}

// confere tipo, tamanho e quantidade antes de enviar: um arquivo grande demais pode derrubar
// a conexão antes de o servidor responder com o erro
function adicionarArquivos(event: Event) {
  const input = event.target as HTMLInputElement
  const arquivos = Array.from(input.files || [])
  input.value = ''

  if (!arquivos.length) return

  const recusados: string[] = []

  for (const arquivo of arquivos) {
    if (!TIPOS_DE_IMAGEM.includes(arquivo.type)) {
      recusados.push(`${arquivo.name} não é PNG, JPEG ou WebP`)
      continue
    }
    if (arquivo.size > TAMANHO_MAXIMO_IMAGEM) {
      recusados.push(`${arquivo.name} passa de 5 MB`)
      continue
    }
    if (imagensMantidas.value.length + arquivosNovos.value.length >= MAXIMO_IMAGENS_POR_ANOTACAO) {
      recusados.push(`${arquivo.name} passa do limite de ${MAXIMO_IMAGENS_POR_ANOTACAO} imagens por anotação`)
      continue
    }

    arquivosNovos.value = [
      ...arquivosNovos.value,
      { id: gerarId(), arquivo, preview: URL.createObjectURL(arquivo) }
    ]
  }

  erroEditor.value = recusados.length ? `Imagens não adicionadas: ${recusados.join('; ')}.` : ''
}

// observa só o id: o pai pode recriar o objeto da aula a cada renderização sem que ela mude
watch(
  () => props.aula.id,
  () => {
    aulaAtual.value = { ...props.aula }
    resetarEditor()
    carregarAnotacoes()
  },
  { immediate: true }
)

function adicionarTag() {
  const valor = novaTag.value.trim().replace(/\s+/g, ' ')
  if (!valor) return

  if (!tagsDisponiveis.value.some(item => mesmaTag(item, valor))) {
    tagsDisponiveis.value = [...tagsDisponiveis.value, valor]
  }

  selecionarTag(valor)
  novaTag.value = ''
}

function removerTag(tag: string) {
  tagsSelecionadas.value = tagsSelecionadas.value.filter(item => item !== tag)
}

function selecionarTag(tag: string) {
  if (!tagsDoEditor.value.some(item => mesmaTag(item, tag))) {
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

onMounted(() => {
  carregarTags()
})

onBeforeUnmount(() => {
  removerListenersPopover()
  liberarArquivosNovos()
})
</script>

<template>
  <div class="area-anotacoes">
    <div class="anotacoes-header">
      <label class="label-visivel" for="anotacoes-aula">Anotações</label>
    </div>

    <p v-if="carregando" class="estado-lista" role="status">Carregando anotações...</p>
    <p v-if="erroLista" class="mensagem-erro" role="alert">{{ erroLista }}</p>

    <div v-if="anotacoes.length" class="lista-anotacoes">
      <article v-for="anotacao in anotacoes" :key="anotacao.id" class="anotacao-card">
        <div v-if="anotacao.tags.length" class="anotacao-cabecalho">
          <div class="chips">
            <span v-for="tag in anotacao.tags" :key="`${anotacao.id}-${tag}`" class="chip chip-selecionado">
              {{ tag }}
            </span>
          </div>
        </div>

        <!-- editar, excluir e mexer nas imagens são só do autor; a API responde 403 aos demais -->
        <div v-if="ehDoUsuario(anotacao)" class="acoes-anotacao">
          <button
            type="button"
            class="acao-anotacao"
            aria-label="Editar anotação"
            title="Editar anotação"
            :disabled="salvando"
            @click="editarAnotacao(anotacao)"
          >
            <PencilLine :size="15" :stroke-width="1.8" aria-hidden="true" />
          </button>
          <button
            type="button"
            class="acao-anotacao acao-excluir"
            aria-label="Excluir anotação"
            title="Excluir anotação"
            :disabled="salvando"
            @click="excluirAnotacao(anotacao)"
          >
            <Trash2 :size="15" :stroke-width="1.8" aria-hidden="true" />
          </button>
        </div>

        <p
          class="texto-anotacao"
          :class="{ 'texto-sem-tags': !anotacao.tags.length && ehDoUsuario(anotacao) }"
        >
          {{ anotacao.conteudo }}
        </p>

        <p v-if="!ehDoUsuario(anotacao)" class="autor-anotacao">por {{ anotacao.autorNome }}</p>

        <div v-if="anotacao.imagens.length" class="previews">
          <div v-for="imagem in anotacao.imagens" :key="imagem.id" class="item-midia imagem-item">
            <ImagemDaAnotacao :imagem="imagem" />
          </div>
        </div>
      </article>
    </div>

    <p v-else-if="!carregando && !erroLista" class="estado-lista">Nenhuma anotação nesta aula ainda.</p>

    <textarea
      id="anotacoes-aula"
      ref="campoAnotacao"
      v-model="texto"
      :disabled="salvando"
      :placeholder="anotacaoEditando ? 'Edite sua anotação...' : 'Escreva suas anotações, ideias, lembretes e observações...'"
      @keydown.enter.ctrl.prevent="salvar"
      @keydown.enter.meta.prevent="salvar"
    />

    <div
      v-if="tagsDoEditor.length"
      class="chips chips-anotacao-atual"
      role="group"
      aria-label="Tags da anotação atual"
    >
      <span
        v-for="tag in tagsDoEditor"
        :key="tag"
        class="chip chip-selecionado"
      >
        {{ tag }}
      </span>
    </div>

    <div v-if="anotacaoEditando" class="estado-edicao">
      <span>Editando anotação</span>
      <button type="button" :disabled="salvando" @click="cancelarEdicaoAnotacao">Cancelar edição</button>
    </div>

    <div class="secao-midia">
      <div class="acoes-contextuais">
        <div class="upload-row">
          <label class="upload-btn" title="Adicionar imagem (PNG, JPEG ou WebP, até 5 MB)">
            <input
              type="file"
              accept="image/png,image/jpeg,image/webp"
              multiple
              aria-label="Adicionar imagem"
              :disabled="salvando"
              @change="adicionarArquivos"
            />
            <ImageIcon :size="15" :stroke-width="1.8" aria-hidden="true" />
          </label>
          <button
            ref="tagButtonRef"
            type="button"
            class="btn-editar-tags"
            :aria-expanded="mostrarEditorTags"
            aria-controls="seletor-tags-aula"
            aria-label="Adicionar tag"
            title="Adicionar tag"
            :disabled="salvando"
            @click="alternarPopoverTags"
          >
            <TagIcon :size="15" :stroke-width="1.8" aria-hidden="true" />
          </button>
          <button type="button" class="btn-salvar" :disabled="salvando" @click="salvar">
            {{ salvando ? 'Salvando...' : (anotacaoEditando ? 'Salvar alterações' : 'Salvar anotação') }}
          </button>
        </div>
        <p class="dica-atalho">Ctrl+Enter também salva.</p>
      </div>

      <p v-if="erroEditor" class="mensagem-erro" role="alert">{{ erroEditor }}</p>

      <div v-if="imagensMantidas.length || arquivosNovos.length" class="previews">
        <div v-for="imagem in imagensMantidas" :key="imagem.id" class="item-midia imagem-item">
          <ImagemDaAnotacao :imagem="imagem" />
          <button type="button" :disabled="salvando" @click="removerImagemSalva(imagem)">Remover</button>
        </div>

        <div v-for="item in arquivosNovos" :key="item.id" class="item-midia imagem-item">
          <img :src="item.preview" :alt="item.arquivo.name" />
          <button type="button" :disabled="salvando" @click="removerArquivoNovo(item.id)">Remover</button>
        </div>
      </div>
    </div>
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

      <div v-if="tagsDoEditor.length" class="chips chips-selecionadas" aria-label="Tags selecionadas">
        <!-- as tags que a anotação já tem não podem ser removidas: a API só acrescenta tags -->
        <span
          v-for="tag in tagsFixas"
          :key="`fixa-${tag}`"
          class="chip chip-selecionado chip-fixo"
          title="Tags já salvas não podem ser removidas"
        >
          {{ tag }}
        </span>
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
            :class="{ 'chip-selecionado': tagsDoEditor.some(item => mesmaTag(item, tag)) }"
            :aria-pressed="tagsDoEditor.some(item => mesmaTag(item, tag))"
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

.btn-salvar {
  min-height: 34px;
  margin-left: auto;
  padding: 0 14px;
  border: 0;
  border-radius: 7px;
  background: #2563eb;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 150ms ease;
}

.btn-salvar:hover {
  background: #1d4ed8;
}

.btn-salvar:disabled,
.acao-anotacao:disabled,
.btn-editar-tags:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.dica-atalho {
  margin: 6px 0 0;
  color: #94a3b8;
  font-size: 11px;
  text-align: right;
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

.chips-anotacao-atual .chip,
.chip-fixo {
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

.autor-anotacao {
  margin: 0;
  color: #64748b;
  font-size: 12px;
}

.estado-lista {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.mensagem-erro {
  margin: 0;
  color: #b91c1c;
  font-size: 13px;
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
.btn-salvar:focus-visible,
.chip:focus-visible {
  outline: 2px solid rgb(37 99 235 / 55%);
  outline-offset: 2px;
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
.item-midia :deep(img) {
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
</style>
