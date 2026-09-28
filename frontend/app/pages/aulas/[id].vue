<template>
  <div class="min-h-screen bg-slate-100 p-6">
    <div class="max-w-md mx-auto space-y-6">

      <button
        class="text-sm text-slate-500 hover:underline"
        @click="router.back()"
      >
        &larr; Voltar
      </button>

      <div>
        <h1 class="text-xl font-semibold text-slate-800">
          {{ aula?.disciplinaNome ?? 'Carregando...' }}
        </h1>

        <p v-if="aula" class="text-sm text-slate-500 mt-1">
          {{ aula.data }} · {{ aula.horario }}
          <span v-if="aula.topico">
            — {{ aula.topico }}
          </span>
        </p>
      </div>

      <form
        class="bg-white rounded-lg shadow p-4 space-y-4"
        @submit.prevent="salvar"
      >
        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">
            Anotação
          </label>

          <textarea
            v-model="conteudo"
            rows="6"
            placeholder="Digite sua anotação..."
            class="w-full rounded-md border px-3 py-2 text-sm outline-none"
            :class="
              erro && !conteudo.trim()
                ? 'border-red-500'
                : 'border-slate-300 focus:border-slate-500'
            "
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-slate-700 mb-1">
            Tags
          </label>

          <div
            class="flex flex-wrap items-center gap-2 rounded-md border border-slate-300 px-2 py-2 focus-within:border-slate-500"
          >
            <span
              v-for="tag in tagsSelecionadas"
              :key="tag"
              class="inline-flex items-center gap-1 rounded-full bg-slate-800 px-2 py-1 text-xs text-white"
            >
              {{ tag }}

              <button
                type="button"
                class="opacity-70 hover:opacity-100"
                @click="removerTag(tag)"
              >
                &times;
              </button>
            </span>

            <input
              v-model="entradaTag"
              type="text"
              placeholder="Digite uma tag"
              class="min-w-[120px] flex-1 border-0 p-1 text-sm outline-none"
              @keydown="aoTeclarTag"
            />
          </div>

          <div
            v-if="tagsSugeridas.length"
            class="mt-3 flex flex-wrap gap-2"
          >
            <button
              v-for="tag in tagsSugeridas"
              :key="tag"
              type="button"
              class="rounded-full border border-slate-300 px-2 py-1 text-xs text-slate-700 hover:bg-slate-50"
              @click="adicionarTag(tag)"
            >
              {{ tag }}
            </button>
          </div>
        </div>

        <p
          v-if="erro"
          class="text-xs text-red-600"
        >
          {{ erro }}
        </p>

        <p
          v-if="sucesso"
          class="text-xs text-green-600"
        >
          {{ sucesso }}
        </p>

        <button
          type="submit"
          :disabled="salvando"
          class="w-full rounded-md bg-slate-800 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-50"
        >
          {{ salvando ? 'Salvando...' : 'Salvar anotação' }}
        </button>
      </form>

    </div>
  </div>
</template>

<script setup lang="ts">
interface Aula {
  id: string
  data: string
  horario: string
  topico: string
  disciplinaNome: string
}

interface Tag {
  id: string
  nome: string
}

const route = useRoute()
const router = useRouter()

const aulaId = route.params.id as string

const { $api } = useNuxtApp()

const aula = ref<Aula | null>(null)

const conteudo = ref('')
const entradaTag = ref('')

const tagsExistentes = ref<string[]>([])
const tagsSelecionadas = ref<string[]>([])

const erro = ref('')
const sucesso = ref('')
const salvando = ref(false)

function normalizar(texto: string) {
  return texto.trim().toLowerCase()
}

const tagsSugeridas = computed(() => {
  const busca = normalizar(entradaTag.value)

  return tagsExistentes.value.filter((tag) => {
    const jaSelecionada = tagsSelecionadas.value.some(
      (selecionada) =>
        normalizar(selecionada) === normalizar(tag)
    )

    const correspondeBusca =
      !busca || normalizar(tag).includes(busca)

    return !jaSelecionada && correspondeBusca
  })
})

function adicionarTag(valor: string) {
  const nome = valor.trim()

  if (!nome) return

  const existente = tagsExistentes.value.find(
    (tag) => normalizar(tag) === normalizar(nome)
  )

  const tagFinal = existente ?? nome

  const jaSelecionada = tagsSelecionadas.value.some(
    (tag) =>
      normalizar(tag) === normalizar(tagFinal)
  )

  if (!jaSelecionada) {
    tagsSelecionadas.value.push(tagFinal)
  }

  entradaTag.value = ''
}

function removerTag(tag: string) {
  tagsSelecionadas.value =
    tagsSelecionadas.value.filter((t) => t !== tag)
}

function aoTeclarTag(evento: KeyboardEvent) {
  if (
    evento.key === 'Enter' ||
    evento.key === ','
  ) {
    evento.preventDefault()
    adicionarTag(entradaTag.value)
  }

  if (
    evento.key === 'Backspace' &&
    !entradaTag.value &&
    tagsSelecionadas.value.length
  ) {
    tagsSelecionadas.value.pop()
  }
}

async function carregarAula() {
  try {
    const { data } =
      await $api.get<Aula>(`/api/aulas/${aulaId}`)

    aula.value = data
  } catch (err) {
    console.error('[aula] erro ao carregar:', err)
    erro.value = 'Não foi possível carregar a aula.'
  }
}

async function carregarTags() {
  try {
    const { data } =
      await $api.get<Tag[]>('/api/tags')

    tagsExistentes.value =
      data.map((tag) => tag.nome)
  } catch (err) {
    console.error('[tags] erro ao carregar:', err)
  }
}

async function salvar() {
  sucesso.value = ''

  if (!conteudo.value.trim()) {
    erro.value = 'Digite o conteúdo da anotação.'
    return
  }

  adicionarTag(entradaTag.value)

  erro.value = ''
  salvando.value = true

  try {
    await $api.post('/api/anotacoes', {
      aulaId,
      autorId: null,
      conteudo: conteudo.value.trim(),
      tags: tagsSelecionadas.value
    })

    conteudo.value = ''
    tagsSelecionadas.value = []
    entradaTag.value = ''

    sucesso.value = 'Anotação salva com sucesso.'

    await carregarTags()
  } catch (err) {
    console.error('[anotacao] erro ao salvar:', err)
    erro.value = 'Não foi possível salvar a anotação.'
  } finally {
    salvando.value = false
  }
}

onMounted(() => {
  carregarAula()
  carregarTags()
})
</script>