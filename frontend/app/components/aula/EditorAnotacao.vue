<script setup lang="ts">
import { Image as ImageIcon, Sparkles, Trash2, X } from '@lucide/vue'
import type { Anotacao, ImagemAnotacao, Tag } from '~/types/anotacao'

// Editor de uma anotação da página da aula. Título e texto são do autor e se salvam sozinhos pouco
// depois da digitação; tags são salvas na hora e qualquer participante pode mexer nelas (SeletorTags).
// Sem `anotacao`, edita uma anotação nova, que só é criada no backend quando ganha título ou texto.

type EstadoSalvamento = 'salvo' | 'pendente' | 'salvando' | 'erro' | 'invalido' | 'rascunho'

const ESPERA_PARA_SALVAR = 800

const props = defineProps<{
  anotacao: Anotacao | null
  tagsDaDisciplina: Tag[]
  // renomear e excluir uma tag afetam todas as anotações da disciplina, então ficam com a página
  renomearTag: (tag: Tag, nome: string) => Promise<void>
  excluirTag: (tag: Tag) => Promise<void>
  // garante que a aula exista no backend; uma aula recorrente é cadastrada na primeira anotação
  obterAulaId: () => Promise<string>
}>()

const emit = defineEmits<{
  // a anotação foi criada ou mudou no backend (título, texto, tags ou imagens)
  salva: [anotacao: Anotacao]
  excluida: [anotacaoId: string]
  // perguntar ao assistente sobre esta anotação
  perguntar: [anotacaoId: string]
}>()

const { criar, editar, excluir, acrescentarTags, removerTag, anexarImagem, removerImagem } = useAnotacoes()
const { usuario } = useAuth()

const titulo = ref(props.anotacao?.titulo ?? '')
const conteudo = ref(props.anotacao?.conteudo ?? '')
const anotacaoId = ref(props.anotacao?.id ?? null)
const estado = ref<EstadoSalvamento>(props.anotacao ? 'salvo' : 'rascunho')
const erroSalvamento = ref('')
const erroAcao = ref('') // erros de imagens e exclusão; os de tags aparecem no SeletorTags
const ocupado = ref(false) // imagem ou exclusão em andamento
const enviandoImagens = ref(false)
const campoTitulo = ref<HTMLInputElement | null>(null)

// o que está salvo no backend, para não reenviar texto que não mudou
let ultimoSalvo = { titulo: titulo.value.trim(), conteudo: conteudo.value.trim() }
let temporizador: ReturnType<typeof setTimeout> | undefined
let fila: Promise<void> = Promise.resolve()

const ehDoUsuario = computed(() =>
  !props.anotacao || (Boolean(usuario.value) && props.anotacao.autorId === usuario.value?.id)
)
const tags = computed(() => props.anotacao?.tags ?? [])
const imagens = computed(() => props.anotacao?.imagens ?? [])
const salvaNoBackend = computed(() => Boolean(anotacaoId.value))

const textoDoEstado = computed(() => {
  switch (estado.value) {
    case 'salvo': return 'Salvo'
    case 'pendente': return 'Alterações ainda não salvas'
    case 'salvando': return 'Salvando...'
    case 'invalido': return 'Uma anotação precisa de título ou texto.'
    case 'erro': return erroSalvamento.value || 'Não foi possível salvar.'
    default: return 'A anotação será salva quando tiver título ou texto.'
  }
})

function formatarDataHora(dataHora: string) {
  const data = new Date(dataHora)
  if (Number.isNaN(data.getTime())) return ''
  return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(data)
}

watch([titulo, conteudo], () => {
  if (!ehDoUsuario.value) return
  estado.value = 'pendente'
  clearTimeout(temporizador)
  temporizador = setTimeout(() => { void salvarAgora() }, ESPERA_PARA_SALVAR)
})

// envia o que estiver pendente; os envios vão um de cada vez, na ordem
function salvarAgora(): Promise<void> {
  clearTimeout(temporizador)
  temporizador = undefined
  fila = fila.then(enviar)
  return fila
}

async function enviar() {
  const tituloAtual = titulo.value.trim()
  const conteudoAtual = conteudo.value.trim()

  if (tituloAtual === ultimoSalvo.titulo && conteudoAtual === ultimoSalvo.conteudo) {
    estado.value = salvaNoBackend.value ? 'salvo' : 'rascunho'
    return
  }
  if (!tituloAtual && !conteudoAtual) {
    // apagar tudo não exclui a anotação: excluir é uma ação separada
    estado.value = salvaNoBackend.value ? 'invalido' : 'rascunho'
    return
  }

  estado.value = 'salvando'
  erroSalvamento.value = ''

  try {
    let salva: Anotacao
    if (anotacaoId.value) {
      salva = await editar(anotacaoId.value, tituloAtual, conteudoAtual)
    }
    else {
      const aulaId = await props.obterAulaId()
      salva = await criar(aulaId, tituloAtual, conteudoAtual, [])
      anotacaoId.value = salva.id
    }
    ultimoSalvo = { titulo: tituloAtual, conteudo: conteudoAtual }
    // se a pessoa digitou durante o envio, o temporizador já agendou o próximo
    estado.value = temporizador ? 'pendente' : 'salvo'
    emit('salva', salva)
  }
  catch (erro) {
    console.error('[anotacao] erro ao salvar:', erro)
    erroSalvamento.value = mensagemDeErro(erro, 'Não foi possível salvar a anotação.')
    estado.value = 'erro'
  }
}

// alterações que ainda não chegaram ao backend
function temPendencia() {
  return Boolean(temporizador) || ['pendente', 'salvando', 'erro', 'invalido'].includes(estado.value)
}

// salva o que estiver pendente antes de sair; devolve se dá para sair sem perder nada
async function prepararSaida() {
  if (!ehDoUsuario.value) return true
  await salvarAgora()
  if (estado.value === 'erro' || estado.value === 'invalido') {
    return window.confirm(`${textoDoEstado.value} Sair mesmo assim e perder as alterações?`)
  }
  return true
}

defineExpose({ salvarAgora, temPendencia, prepararSaida })

// o assistente lê a anotação do backend, então o texto pendente é salvo antes
async function perguntarAoAssistente() {
  if (!anotacaoId.value) return
  if (ehDoUsuario.value) await salvarAgora()
  emit('perguntar', anotacaoId.value)
}

// o SeletorTags mostra os erros destas ações
async function acrescentarTag(nome: string) {
  if (!anotacaoId.value) return
  emit('salva', await acrescentarTags(anotacaoId.value, [nome]))
}

async function tirarTag(nome: string) {
  if (!anotacaoId.value) return
  emit('salva', await removerTag(anotacaoId.value, nome))
}

// confere tipo, tamanho e quantidade antes de enviar, e envia uma imagem por vez
async function anexarArquivos(event: Event) {
  const input = event.target as HTMLInputElement
  const arquivos = Array.from(input.files || [])
  input.value = ''
  if (!arquivos.length || !props.anotacao || !anotacaoId.value) return

  enviandoImagens.value = true
  erroAcao.value = ''
  const recusados: string[] = []
  let atual = props.anotacao

  for (const arquivo of arquivos) {
    if (!TIPOS_DE_IMAGEM.includes(arquivo.type)) {
      recusados.push(`${arquivo.name} não é PNG, JPEG ou WebP`)
      continue
    }
    if (arquivo.size > TAMANHO_MAXIMO_IMAGEM) {
      recusados.push(`${arquivo.name} passa de 5 MB`)
      continue
    }
    if (atual.imagens.length >= MAXIMO_IMAGENS_POR_ANOTACAO) {
      recusados.push(`${arquivo.name} passa do limite de ${MAXIMO_IMAGENS_POR_ANOTACAO} imagens por anotação`)
      continue
    }
    try {
      const imagem = await anexarImagem(anotacaoId.value, arquivo)
      atual = { ...atual, imagens: [...atual.imagens, imagem] }
      emit('salva', atual)
    }
    catch (erro) {
      console.error('[anotacao] erro ao enviar imagem:', erro)
      recusados.push(`${arquivo.name} (${mensagemDeErro(erro, 'falha no envio')})`)
    }
  }

  enviandoImagens.value = false
  if (recusados.length) {
    erroAcao.value = `Imagens não enviadas: ${recusados.join('; ')}.`
  }
}

async function tirarImagem(imagem: ImagemAnotacao) {
  if (!props.anotacao || ocupado.value || !window.confirm('Deseja remover esta imagem da anotação?')) return

  ocupado.value = true
  erroAcao.value = ''
  try {
    await removerImagem(imagem)
    emit('salva', { ...props.anotacao, imagens: props.anotacao.imagens.filter(item => item.id !== imagem.id) })
  }
  catch (erro) {
    console.error('[anotacao] erro ao remover imagem:', erro)
    erroAcao.value = mensagemDeErro(erro, 'Não foi possível remover a imagem.')
  }
  finally {
    ocupado.value = false
  }
}

async function excluirAnotacao() {
  if (!anotacaoId.value || ocupado.value || !window.confirm('Deseja excluir esta anotação?')) return

  // o que estiver para ser salvo deixa de importar
  clearTimeout(temporizador)
  temporizador = undefined
  await fila

  ocupado.value = true
  erroAcao.value = ''
  try {
    await excluir(anotacaoId.value)
    emit('excluida', anotacaoId.value)
  }
  catch (erro) {
    console.error('[anotacao] erro ao excluir:', erro)
    erroAcao.value = mensagemDeErro(erro, 'Não foi possível excluir a anotação.')
    ocupado.value = false
  }
}

onMounted(() => {
  // uma anotação nova já abre pronta para digitar
  if (!props.anotacao) campoTitulo.value?.focus()
})

onBeforeUnmount(() => {
  // trocar de anotação já salva antes (prepararSaida); isto cobre a página sendo desmontada às pressas
  if (temporizador) void salvarAgora()
})
</script>

<template>
  <section class="editor" aria-label="Editor da anotação">
    <input
      ref="campoTitulo"
      v-model="titulo"
      type="text"
      class="campo-titulo"
      aria-label="Título da anotação"
      placeholder="Título"
      :maxlength="TAMANHO_MAXIMO_TITULO"
      :readonly="!ehDoUsuario"
    />

    <p class="linha-meta">
      <template v-if="anotacao">
        <span>{{ ehDoUsuario ? 'Sua anotação' : `por ${anotacao.autorNome}` }}</span>
        <span v-if="formatarDataHora(anotacao.dataCriacao)">· criada em {{ formatarDataHora(anotacao.dataCriacao) }}</span>
      </template>
      <span v-else>Nova anotação</span>
      <span class="acoes-meta">
        <span
          v-if="ehDoUsuario"
          class="estado-salvamento"
          :class="`estado-${estado}`"
          role="status"
        >
          {{ textoDoEstado }}
          <button v-if="estado === 'erro'" type="button" class="link-acao" @click="salvarAgora">Tentar de novo</button>
        </span>
        <button
          v-if="salvaNoBackend"
          type="button"
          class="perguntar-assistente"
          title="Perguntar ao assistente sobre esta anotação"
          @click="perguntarAoAssistente"
        >
          <Sparkles :size="13" :stroke-width="2" aria-hidden="true" />
          Perguntar ao assistente
        </button>
      </span>
    </p>

    <textarea
      v-model="conteudo"
      class="campo-conteudo"
      aria-label="Texto da anotação"
      :placeholder="ehDoUsuario ? 'Escreva suas anotações, ideias, lembretes e observações...' : ''"
      :readonly="!ehDoUsuario"
    />

    <SeletorTags
      :selecionadas="tags"
      :opcoes="tagsDaDisciplina"
      :desabilitado="!salvaNoBackend"
      dica-desabilitado="Escreva um título ou texto antes de acrescentar tags."
      :acrescentar="acrescentarTag"
      :remover="tirarTag"
      :renomear="renomearTag"
      :excluir="excluirTag"
    />

    <div v-if="imagens.length || ehDoUsuario" class="secao-imagens">
      <ul v-if="imagens.length" class="imagens-anexadas" aria-label="Imagens anexadas à anotação">
        <li v-for="imagem in imagens" :key="imagem.id" class="imagem-anexada">
          <ImagemDaAnotacao :imagem="imagem" />
          <button
            v-if="ehDoUsuario"
            type="button"
            class="remover-imagem"
            :aria-label="`Remover imagem ${imagem.nome}`"
            title="Remover imagem"
            :disabled="ocupado"
            @click="tirarImagem(imagem)"
          >
            <X :size="14" :stroke-width="2" aria-hidden="true" />
          </button>
        </li>
      </ul>
      <label
        v-if="ehDoUsuario"
        class="botao-secundario anexar"
        :class="{ desabilitado: !salvaNoBackend || enviandoImagens }"
        :title="salvaNoBackend ? 'PNG, JPEG ou WebP, até 5 MB' : 'Escreva um título ou texto primeiro'"
      >
        <input
          type="file"
          accept="image/png,image/jpeg,image/webp"
          multiple
          aria-label="Anexar imagem à anotação"
          :disabled="!salvaNoBackend || enviandoImagens"
          @change="anexarArquivos"
        />
        <ImageIcon :size="15" :stroke-width="1.8" aria-hidden="true" />
        {{ enviandoImagens ? 'Enviando...' : 'Anexar imagem' }}
      </label>
    </div>

    <p v-if="erroAcao" class="mensagem-erro" role="alert">{{ erroAcao }}</p>

    <footer v-if="ehDoUsuario && salvaNoBackend" class="rodape-editor">
      <button type="button" class="botao-excluir" :disabled="ocupado" @click="excluirAnotacao">
        <Trash2 :size="15" :stroke-width="1.8" aria-hidden="true" />
        Excluir anotação
      </button>
    </footer>
  </section>
</template>

<style scoped>
.editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
  min-height: 0;
  padding: 20px 24px;
  overflow-y: auto;
  box-sizing: border-box;
}

.campo-titulo {
  width: 100%;
  padding: 4px 0;
  border: 0;
  border-bottom: 1px solid transparent;
  background: transparent;
  color: #0f172a;
  font: inherit;
  font-size: 22px;
  font-weight: 700;
}

.campo-titulo:focus {
  outline: none;
  border-bottom-color: #bfdbfe;
}

.campo-titulo::placeholder {
  color: #94a3b8;
}

.linha-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin: -6px 0 0;
  color: #64748b;
  font-size: 12px;
}

.acoes-meta {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.estado-salvamento {
  color: #64748b;
}

.perguntar-assistente {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 9px;
  border: 1px solid #ddd6fe;
  border-radius: 999px;
  background: #fff;
  color: #6d28d9;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.perguntar-assistente:hover {
  background: #f5f3ff;
}

.estado-salvo {
  color: #15803d;
}

.estado-erro,
.estado-invalido {
  color: #b91c1c;
}

.link-acao {
  margin-left: 6px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #2563eb;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.campo-conteudo {
  flex: 1 0 220px;
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  color: #0f172a;
  font: inherit;
  font-size: 15px;
  line-height: 1.6;
  resize: none;
  box-sizing: border-box;
}

.campo-conteudo:focus {
  outline: 2px solid #bfdbfe;
  outline-offset: 1px;
}

.campo-conteudo:read-only,
.campo-titulo:read-only {
  cursor: default;
}

.botao-secundario {
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

.botao-secundario:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
}

.botao-secundario:disabled,
.botao-secundario.desabilitado {
  opacity: 0.55;
  cursor: not-allowed;
}

.secao-imagens {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 10px;
}

.imagens-anexadas {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.imagem-anexada {
  position: relative;
  width: 140px;
  height: 100px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #e2e8f0;
}

.imagem-anexada :deep(img),
.imagem-anexada :deep(span) {
  width: 100%;
  height: 100%;
  min-height: 0;
  object-fit: cover;
}

.remover-imagem {
  position: absolute;
  top: 4px;
  right: 4px;
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: rgb(15 23 42 / 70%);
  color: #fff;
  cursor: pointer;
}

.anexar {
  position: relative;
}

.anexar input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.anexar.desabilitado input {
  cursor: not-allowed;
}

.mensagem-erro {
  margin: 0;
  color: #b91c1c;
  font-size: 13px;
}

.rodape-editor {
  display: flex;
  justify-content: flex-end;
  padding-top: 4px;
}

.botao-excluir {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid transparent;
  border-radius: 7px;
  background: transparent;
  color: #b91c1c;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.botao-excluir:hover {
  border-color: #fee2e2;
  background: #fef2f2;
}

.botao-excluir:disabled,
.remover-imagem:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.botao-secundario:focus-visible,
.botao-excluir:focus-visible,
.remover-imagem:focus-visible,
.link-acao:focus-visible,
.perguntar-assistente:focus-visible,
.anexar:focus-within {
  outline: 2px solid rgb(37 99 235 / 55%);
  outline-offset: 2px;
}
</style>
