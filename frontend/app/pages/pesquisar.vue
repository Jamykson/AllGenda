<script setup lang="ts">
import { BookOpen, CalendarDays, ChevronDown, Clock3, Image as ImageIcon, Search, Tag as TagIcon, UserRound, X } from '@lucide/vue'
import type { LocationQuery } from 'vue-router'
import type { AnotacaoPesquisa, AulaPesquisa, FiltroAutor } from '~/composables/usePesquisa'

definePageMeta({
  layout: 'default'
})

const OPCOES_AUTOR: { valor: FiltroAutor, rotulo: string }[] = [
  { valor: 'todos', rotulo: 'Todos' },
  { valor: 'minhas', rotulo: 'Minhas' },
  { valor: 'colegas', rotulo: 'Dos colegas' }
]

const route = useRoute()
const router = useRouter()
const { usuario } = useAuth()

const textoBusca = ref('')
const disciplinaId = ref('')
const autor = ref<FiltroAutor>('todos')
const dataInicial = ref('')
const dataFinal = ref('')
const horaInicial = ref('')
const horaFinal = ref('')
const tagsSelecionadas = ref<string[]>([])
const buscaTag = ref('')
const barraFiltrosRef = ref<HTMLElement | null>(null)
const filtroAberto = ref<'disciplina' | 'autor' | 'data' | 'horario' | 'tags' | null>(null)

const {
  disciplinas,
  tagsDisponiveis,
  carregando,
  carregandoColegas,
  erro,
  problemaDisciplina,
  resultados,
  carregar
} = usePesquisa({ disciplinaId, autor })

// A disciplina, o autor e as tags ficam na URL: a página da disciplina abre a pesquisa já filtrada,
// e recarregar a página mantém os filtros.
function textoDaQuery(valor: LocationQuery[string] | undefined) {
  const primeiro = Array.isArray(valor) ? valor[0] : valor
  return typeof primeiro === 'string' ? primeiro : ''
}

function lerFiltrosDaUrl(query: LocationQuery) {
  disciplinaId.value = textoDaQuery(query.disciplinaId)
  const autorDaUrl = textoDaQuery(query.autor)
  autor.value = OPCOES_AUTOR.some(opcao => opcao.valor === autorDaUrl) ? autorDaUrl as FiltroAutor : 'todos'
  const tags = Array.isArray(query.tag) ? query.tag : [query.tag]
  tagsSelecionadas.value = [...new Set(tags.filter((tag): tag is string => typeof tag === 'string' && Boolean(tag.trim())))]
}

function queryDosFiltros(): LocationQuery {
  return {
    ...(disciplinaId.value ? { disciplinaId: disciplinaId.value } : {}),
    ...(autor.value !== 'todos' ? { autor: autor.value } : {}),
    ...(tagsSelecionadas.value.length ? { tag: tagsSelecionadas.value } : {})
  }
}

// compara por valor, para que escrever a URL a partir dos filtros não os leia de volta
function mesmaQuery(queryA: LocationQuery, queryB: LocationQuery) {
  const normalizar = (query: LocationQuery) => JSON.stringify({
    disciplinaId: textoDaQuery(query.disciplinaId),
    autor: textoDaQuery(query.autor),
    tag: (Array.isArray(query.tag) ? query.tag : [query.tag]).filter(Boolean)
  })
  return normalizar(queryA) === normalizar(queryB)
}

lerFiltrosDaUrl(route.query)
const caminhoDaPesquisa = route.path

// os dois watches só valem enquanto a rota é a da pesquisa: ao abrir uma aula, a rota muda antes
// de esta página sair da tela
watch([disciplinaId, autor, tagsSelecionadas], () => {
  const query = queryDosFiltros()
  if (route.path === caminhoDaPesquisa && !mesmaQuery(query, route.query)) void router.replace({ query })
})

// a URL muda por fora quando, por exemplo, o link "Pesquisar" da barra lateral é clicado aqui mesmo
watch(() => route.query, (query) => {
  if (route.path === caminhoDaPesquisa && !mesmaQuery(query, queryDosFiltros())) lerFiltrosDaUrl(query)
})

onMounted(() => {
  void carregar()
  document.addEventListener('pointerdown', fecharFiltroAoClicarFora)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', fecharFiltroAoClicarFora)
})

function fecharFiltroAoClicarFora(evento: PointerEvent) {
  const alvo = evento.target
  if (alvo instanceof Node && !barraFiltrosRef.value?.contains(alvo)) {
    filtroAberto.value = null
  }
}

function alternarFiltro(filtro: NonNullable<typeof filtroAberto.value>) {
  filtroAberto.value = filtroAberto.value === filtro ? null : filtro
}

function normalizarTexto(texto: string) {
  return texto
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLocaleLowerCase('pt-BR')
    .trim()
}

function minutos(horario: string) {
  const correspondencia = /^(\d{1,2}):(\d{2})$/.exec(horario)
  if (!correspondencia) return null

  const hora = Number(correspondencia[1])
  const minuto = Number(correspondencia[2])
  if (hora > 23 || minuto > 59) return null
  return hora * 60 + minuto
}

function faixaDeHorario(horario: string) {
  const valores = horario.match(/\d{1,2}:\d{2}/g) || []
  if (valores.length < 2) return null

  const inicio = minutos(valores[0] || '')
  const fim = minutos(valores[1] || '')
  if (inicio === null || fim === null || fim < inicio) return null

  return { inicio, fim }
}

function correspondeHorario(horario: string) {
  const limiteInicio = horaInicial.value ? minutos(horaInicial.value) : null
  const limiteFim = horaFinal.value ? minutos(horaFinal.value) : null
  if (limiteInicio === null && limiteFim === null) return true

  const faixa = faixaDeHorario(horario)
  if (!faixa) return false

  return (limiteInicio === null || faixa.fim >= limiteInicio)
    && (limiteFim === null || faixa.inicio <= limiteFim)
}

const resultadosFiltrados = computed(() => {
  const consulta = normalizarTexto(textoBusca.value)
  const tagsNormalizadas = tagsSelecionadas.value.map(normalizarTexto)

  return resultados.value
    .filter(({ aula, anotacao }) => {
      if (consulta && !normalizarTexto(`${anotacao.titulo}\n${anotacao.texto}`).includes(consulta)) {
        return false
      }

      if (disciplinaId.value && aula.disciplinaId !== disciplinaId.value) {
        return false
      }

      if (autor.value === 'minhas' && anotacao.autorId !== usuario.value?.id) return false

      if (dataInicial.value && aula.data < dataInicial.value) return false
      if (dataFinal.value && aula.data > dataFinal.value) return false
      if (!correspondeHorario(aula.horario)) return false

      const tagsDaAnotacao = new Set(anotacao.tags.map(normalizarTexto))
      return tagsNormalizadas.every(tag => tagsDaAnotacao.has(tag))
    })
    .sort((resultadoA, resultadoB) =>
      resultadoB.aula.data.localeCompare(resultadoA.aula.data)
      || resultadoB.aula.horario.localeCompare(resultadoA.aula.horario)
    )
})

interface GrupoDaAula {
  aula: AulaPesquisa
  anotacoes: AnotacaoPesquisa[]
}

// os resultados já vêm da aula mais recente para a mais antiga, então cada aula é uma sequência
const gruposFiltrados = computed(() => {
  const grupos: GrupoDaAula[] = []
  for (const { aula, anotacao } of resultadosFiltrados.value) {
    const ultimo = grupos.at(-1)
    if (ultimo?.aula.id === aula.id) ultimo.anotacoes.push(anotacao)
    else grupos.push({ aula, anotacoes: [anotacao] })
  }
  return grupos
})

const resumoResultados = computed(() => {
  const anotacoes = resultadosFiltrados.value.length
  const aulas = gruposFiltrados.value.length
  return `${anotacoes} ${anotacoes === 1 ? 'anotação' : 'anotações'} em ${aulas} ${aulas === 1 ? 'aula' : 'aulas'}`
})

const nomeDisciplinaSelecionada = computed(() => {
  if (!disciplinaId.value) return 'Todas as disciplinas'
  return disciplinas.value.find(item => item.id === disciplinaId.value)?.nome
    ?? (carregando.value ? 'Carregando...' : 'Disciplina indisponível')
})

const rotuloAutor = computed(() =>
  autor.value === 'todos' ? 'Autor' : OPCOES_AUTOR.find(opcao => opcao.valor === autor.value)?.rotulo
)

const filtrosAtivos = computed(() => Boolean(
  textoBusca.value.trim()
  || disciplinaId.value
  || autor.value !== 'todos'
  || dataInicial.value
  || dataFinal.value
  || horaInicial.value
  || horaFinal.value
  || tagsSelecionadas.value.length
))

const tagsFiltradas = computed(() => {
  const busca = normalizarTexto(buscaTag.value)
  return tagsDisponiveis.value.filter(tag =>
    !busca || normalizarTexto(tag).includes(busca)
  )
})

function alternarTag(tag: string) {
  if (tagsSelecionadas.value.includes(tag)) {
    tagsSelecionadas.value = tagsSelecionadas.value.filter(item => item !== tag)
    return
  }

  tagsSelecionadas.value = [...tagsSelecionadas.value, tag]
}

function limparFiltros() {
  textoBusca.value = ''
  disciplinaId.value = ''
  autor.value = 'todos'
  dataInicial.value = ''
  dataFinal.value = ''
  horaInicial.value = ''
  horaFinal.value = ''
  tagsSelecionadas.value = []
  buscaTag.value = ''
}

function formatarData(data: string) {
  const [ano, mes, dia] = data.split('-').map(Number)
  if (!ano || !mes || !dia) return data

  return new Intl.DateTimeFormat('pt-BR', {
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  }).format(new Date(ano, mes - 1, dia))
}

// voltar da página da aula remonta a pesquisa, que recarrega os resultados
function abrirAula(aula: AulaPesquisa) {
  navigateTo(rotaDaAula(aula))
}

// abre a aula já com a anotação selecionada no editor
function abrirAnotacao(aula: AulaPesquisa, anotacao: AnotacaoPesquisa) {
  if (aula.recorrente) {
    abrirAula(aula)
    return
  }
  navigateTo({ path: `/aulas/${aula.id}`, query: { anotacao: anotacao.id } })
}

function autorDaAnotacao(anotacao: AnotacaoPesquisa) {
  return usuario.value && anotacao.autorId === usuario.value.id ? 'Você' : `por ${anotacao.autorNome}`
}
</script>

<template>
  <main class="pagina-pesquisa">
    <header class="cabecalho-pesquisa">
      <h1>Pesquisar</h1>
    </header>

    <label class="campo-busca">
      <Search :size="17" :stroke-width="1.8" aria-hidden="true" />
      <span class="visually-hidden">Pesquisar nas anotações</span>
      <input
        v-model="textoBusca"
        type="search"
        placeholder="Pesquisar nas anotações..."
        autocomplete="off"
      >
    </label>

    <section ref="barraFiltrosRef" class="barra-filtros" aria-label="Filtros da pesquisa">
      <div class="filtro-wrapper">
        <button
          type="button"
          class="botao-filtro"
          :aria-expanded="filtroAberto === 'disciplina'"
          aria-controls="painel-filtro-disciplina"
          @click="alternarFiltro('disciplina')"
        >
          <BookOpen :size="15" :stroke-width="1.8" aria-hidden="true" />
          <span>{{ nomeDisciplinaSelecionada }}</span>
          <ChevronDown :size="14" :stroke-width="1.8" aria-hidden="true" />
        </button>
        <div v-if="filtroAberto === 'disciplina'" id="painel-filtro-disciplina" class="painel-filtro painel-disciplinas">
          <button
            type="button"
            class="opcao-disciplina"
            :class="{ ativa: !disciplinaId }"
            :aria-pressed="!disciplinaId"
            @click="disciplinaId = ''; filtroAberto = null"
          >
            Todas as disciplinas
          </button>
          <button
            v-for="disciplina in disciplinas"
            :key="disciplina.id"
            type="button"
            class="opcao-disciplina"
            :class="{ ativa: disciplinaId === disciplina.id }"
            :aria-pressed="disciplinaId === disciplina.id"
            @click="disciplinaId = disciplina.id; filtroAberto = null"
          >
            {{ disciplina.nome }}
          </button>
        </div>
      </div>

      <div class="filtro-wrapper">
        <button
          type="button"
          class="botao-filtro"
          :aria-expanded="filtroAberto === 'autor'"
          aria-controls="painel-filtro-autor"
          @click="alternarFiltro('autor')"
        >
          <UserRound :size="15" :stroke-width="1.8" aria-hidden="true" />
          {{ rotuloAutor }}
          <ChevronDown :size="14" :stroke-width="1.8" aria-hidden="true" />
        </button>
        <div v-if="filtroAberto === 'autor'" id="painel-filtro-autor" class="painel-filtro painel-disciplinas">
          <button
            v-for="opcao in OPCOES_AUTOR"
            :key="opcao.valor"
            type="button"
            class="opcao-disciplina"
            :class="{ ativa: autor === opcao.valor }"
            :aria-pressed="autor === opcao.valor"
            :disabled="opcao.valor === 'minhas' && !usuario"
            @click="autor = opcao.valor; filtroAberto = null"
          >
            {{ opcao.rotulo }}
          </button>
        </div>
      </div>

      <div class="filtro-wrapper">
        <button
          type="button"
          class="botao-filtro"
          :aria-expanded="filtroAberto === 'data'"
          aria-controls="painel-filtro-data"
          @click="alternarFiltro('data')"
        >
          <CalendarDays :size="15" :stroke-width="1.8" aria-hidden="true" />
          Data
          <span v-if="dataInicial || dataFinal" class="contador-filtro">•</span>
          <ChevronDown :size="14" :stroke-width="1.8" aria-hidden="true" />
        </button>
        <div v-if="filtroAberto === 'data'" id="painel-filtro-data" class="painel-filtro painel-data">
          <label>
            <span>Data inicial</span>
            <input v-model="dataInicial" type="date" aria-label="Data inicial">
          </label>
          <label>
            <span>Data final</span>
            <input v-model="dataFinal" type="date" aria-label="Data final">
          </label>
        </div>
      </div>

      <div class="filtro-wrapper">
        <button
          type="button"
          class="botao-filtro"
          :aria-expanded="filtroAberto === 'horario'"
          aria-controls="painel-filtro-horario"
          @click="alternarFiltro('horario')"
        >
          <Clock3 :size="15" :stroke-width="1.8" aria-hidden="true" />
          Horário
          <span v-if="horaInicial || horaFinal" class="contador-filtro">•</span>
          <ChevronDown :size="14" :stroke-width="1.8" aria-hidden="true" />
        </button>
        <div v-if="filtroAberto === 'horario'" id="painel-filtro-horario" class="painel-filtro painel-data">
          <label>
            <span>Hora inicial</span>
            <input v-model="horaInicial" type="time" aria-label="Hora inicial">
          </label>
          <label>
            <span>Hora final</span>
            <input v-model="horaFinal" type="time" aria-label="Hora final">
          </label>
        </div>
      </div>

      <div class="filtro-wrapper">
        <button
          type="button"
          class="botao-filtro"
          :aria-expanded="filtroAberto === 'tags'"
          aria-controls="painel-filtro-tags"
          @click="alternarFiltro('tags')"
        >
          <TagIcon :size="15" :stroke-width="1.8" aria-hidden="true" />
          Tags
          <span v-if="tagsSelecionadas.length" class="contador-filtro">
            {{ tagsSelecionadas.length }}
          </span>
          <ChevronDown :size="14" :stroke-width="1.8" aria-hidden="true" />
        </button>
        <div v-if="filtroAberto === 'tags'" id="painel-filtro-tags" class="painel-filtro painel-tags">
          <label class="busca-tags">
            <span class="visually-hidden">Pesquisar tags</span>
            <Search :size="14" :stroke-width="1.8" aria-hidden="true" />
            <input v-model="buscaTag" type="search" placeholder="Pesquisar tags...">
          </label>
          <div class="opcoes-tags">
            <button
              v-for="tag in tagsFiltradas"
              :key="tag"
              type="button"
              class="chip-tag"
              :class="{ selecionada: tagsSelecionadas.includes(tag) }"
              :aria-pressed="tagsSelecionadas.includes(tag)"
              @click="alternarTag(tag)"
            >
              {{ tag }}
            </button>
            <span v-if="!tagsFiltradas.length" class="sem-tags">Nenhuma tag encontrada.</span>
          </div>
        </div>
      </div>

      <button v-if="filtrosAtivos" type="button" class="limpar-filtros" @click="limparFiltros">
        Limpar filtros
      </button>
    </section>

    <div v-if="tagsSelecionadas.length" class="tags-ativas" aria-label="Tags incluídas na pesquisa">
      <button
        v-for="tag in tagsSelecionadas"
        :key="tag"
        type="button"
        class="chip-tag selecionada"
        :aria-label="`Remover filtro de tag ${tag}`"
        title="Remover filtro de tag"
        @click="alternarTag(tag)"
      >
        {{ tag }} <X :size="13" :stroke-width="2" aria-hidden="true" />
      </button>
    </div>

    <section class="secao-resultados" aria-live="polite">
      <header class="cabecalho-resultados">
        <h2>Resultados</h2>
        <span v-if="!carregando && !carregandoColegas && !erro && !problemaDisciplina">{{ resumoResultados }}</span>
      </header>

      <p v-if="carregando" class="estado-pesquisa" role="status">
        Carregando dados para pesquisa...
      </p>

      <p v-else-if="erro" class="estado-erro" role="alert">
        {{ erro }}
      </p>

      <div v-else-if="problemaDisciplina" class="estado-pesquisa estado-disciplina" role="alert">
        <template v-if="problemaDisciplina === 'sem-acesso'">
          <strong>Você não participa desta disciplina.</strong>
          <span>
            Só participantes veem as anotações dela. Se você recebeu um convite, aceite-o em
            <NuxtLink to="/convites">Convites</NuxtLink>.
          </span>
        </template>
        <strong v-else>Esta disciplina não existe ou foi excluída.</strong>
        <button type="button" class="limpar-filtros" @click="disciplinaId = ''">
          Ver todas as disciplinas
        </button>
      </div>

      <p v-else-if="carregandoColegas" class="estado-pesquisa" role="status">
        Carregando anotações dos colegas...
      </p>

      <p v-else-if="!resultadosFiltrados.length && autor === 'colegas'" class="estado-pesquisa">
        Nenhuma anotação dos colegas encontrada.
      </p>

      <p v-else-if="!resultadosFiltrados.length && filtrosAtivos" class="estado-pesquisa">
        Nenhum resultado encontrado. Tente remover alguns filtros ou pesquisar outro termo.
      </p>

      <p v-else-if="!resultadosFiltrados.length" class="estado-pesquisa">
        <strong>Nenhuma anotação encontrada.</strong>
        <span>Suas anotações aparecerão aqui quando forem adicionadas às aulas.</span>
      </p>

      <div v-else class="lista-grupos">
        <section
          v-for="grupo in gruposFiltrados"
          :key="grupo.aula.id"
          class="grupo-aula"
          :aria-label="`Aula de ${grupo.aula.disciplinaNome}, ${formatarData(grupo.aula.data)}`"
        >
          <button
            type="button"
            class="cabecalho-grupo"
            :title="`Abrir a aula de ${formatarData(grupo.aula.data)}`"
            @click="abrirAula(grupo.aula)"
          >
            <span class="linha-disciplina">
              <span
                class="indicador-disciplina"
                :style="{ '--cor-disciplina': grupo.aula.disciplinaCor || '#94a3b8' }"
                aria-hidden="true"
              />
              <span v-if="grupo.aula.disciplinaIcone" class="icone-disciplina" aria-hidden="true">
                {{ grupo.aula.disciplinaIcone }}
              </span>
              <strong>{{ grupo.aula.disciplinaNome }}</strong>
            </span>
            <span class="data-horario">
              {{ formatarData(grupo.aula.data) }} · {{ grupo.aula.horario }}
            </span>
            <span v-if="grupo.aula.topico" class="topico-aula">{{ grupo.aula.topico }}</span>
          </button>

          <div class="lista-resultados">
            <button
              v-for="anotacao in grupo.anotacoes"
              :key="anotacao.id"
              type="button"
              class="resultado"
              :aria-label="`Abrir ${anotacao.titulo ? `a anotação ${anotacao.titulo}` : 'a anotação'}, ${autorDaAnotacao(anotacao)}`"
              @click="abrirAnotacao(grupo.aula, anotacao)"
            >
              <span v-if="anotacao.titulo" class="titulo-anotacao">
                {{ anotacao.titulo }}
              </span>

              <span v-if="anotacao.texto.trim()" class="trecho-anotacao">
                {{ anotacao.texto }}
              </span>

              <span class="rodape-resultado">
                <span v-if="anotacao.tags.length" class="tags-resultado">
                  <span v-for="tag in anotacao.tags" :key="tag" class="chip-tag">
                    {{ tag }}
                  </span>
                </span>
                <span v-else class="tags-resultado-vazias" />

                <span class="autor-anotacao">{{ autorDaAnotacao(anotacao) }}</span>

                <span v-if="anotacao.imagens.length" class="indicadores-midia">
                  <span :title="`${anotacao.imagens.length} imagem(ns)`">
                    <ImageIcon :size="14" :stroke-width="1.8" aria-hidden="true" />
                    <span class="visually-hidden">Possui imagem</span>
                  </span>
                </span>
              </span>
            </button>
          </div>
        </section>
      </div>
    </section>
  </main>
</template>

<style scoped>
.pagina-pesquisa {
  width: 100%;
  min-width: 0;
  color: #1f2937;
}

.cabecalho-pesquisa {
  margin-bottom: 16px;
}

.cabecalho-pesquisa h1 {
  font-size: 20px;
  font-weight: 700;
}

.campo-busca {
  display: flex;
  width: 100%;
  height: 42px;
  align-items: center;
  gap: 10px;
  box-sizing: border-box;
  padding: 0 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  color: #64748b;
}

.campo-busca:focus-within {
  border-color: #60a5fa;
  box-shadow: 0 0 0 3px rgb(37 99 235 / 12%);
}

.campo-busca input,
.busca-tags input {
  width: 100%;
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: #1f2937;
  font: inherit;
  font-size: 14px;
}

.campo-busca input::placeholder,
.busca-tags input::placeholder {
  color: #94a3b8;
  opacity: 1;
}

.barra-filtros {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
}

.botao-filtro,
.limpar-filtros {
  display: inline-flex;
  min-height: 34px;
  align-items: center;
  gap: 7px;
  padding: 0 10px;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  background: #fff;
  color: #475569;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
}

.filtro-wrapper {
  position: relative;
}

.botao-filtro:hover,
.botao-filtro[aria-expanded="true"],
.limpar-filtros:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
}

.botao-filtro:focus-visible,
.opcao-disciplina:focus-visible {
  outline: 2px solid rgb(37 99 235 / 55%);
  outline-offset: 2px;
}

.contador-filtro {
  color: #2563eb;
  font-weight: 700;
}

.painel-filtro {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  z-index: 10;
  width: min(280px, calc(100vw - 32px));
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 9px;
  background: #fff;
  box-shadow: 0 8px 24px rgb(15 23 42 / 10%);
}

.painel-data {
  display: grid;
  gap: 10px;
}

.painel-disciplinas {
  display: grid;
  max-height: min(280px, calc(100dvh - 120px));
  overflow-y: auto;
  gap: 2px;
}

.opcao-disciplina {
  min-height: 32px;
  padding: 0 8px;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: #475569;
  font: inherit;
  font-size: 12px;
  text-align: left;
  cursor: pointer;
}

.opcao-disciplina:hover:not(:disabled),
.opcao-disciplina.ativa {
  background: #eff6ff;
  color: #1d4ed8;
}

.opcao-disciplina:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.painel-data label {
  display: grid;
  gap: 5px;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
}

.painel-data input {
  width: 100%;
  height: 34px;
  box-sizing: border-box;
  padding: 0 8px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  color: #334155;
  font: inherit;
}

.painel-data input:focus-visible,
.botao-filtro:focus-visible,
.busca-tags:focus-within,
.chip-tag:focus-visible,
.resultado:focus-visible,
.limpar-filtros:focus-visible {
  outline: 2px solid rgb(37 99 235 / 55%);
  outline-offset: 2px;
}

.painel-tags {
  width: min(300px, calc(100vw - 32px));
}

.busca-tags {
  display: flex;
  height: 34px;
  align-items: center;
  gap: 7px;
  padding: 0 8px;
  border-bottom: 1px solid #eef2f7;
  color: #64748b;
}

.busca-tags input {
  font-size: 12px;
}

.opcoes-tags,
.tags-ativas,
.tags-resultado {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.opcoes-tags {
  max-height: 180px;
  overflow-y: auto;
  padding-top: 2px;
}

.chip-tag {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  font-weight: 500;
}

button.chip-tag {
  cursor: pointer;
}

.chip-tag.selecionada {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}

.sem-tags {
  color: #64748b;
  font-size: 12px;
}

.limpar-filtros {
  cursor: pointer;
}

.tags-ativas {
  margin-top: 10px;
}

.secao-resultados {
  margin-top: 20px;
}

.cabecalho-resultados {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 10px;
}

.cabecalho-resultados h2 {
  color: #334155;
  font-size: 14px;
  font-weight: 700;
}

.cabecalho-resultados > span {
  color: #64748b;
  font-size: 12px;
}

.lista-grupos {
  display: grid;
  gap: 18px;
}

.grupo-aula {
  display: grid;
  min-width: 0;
  gap: 8px;
}

.cabecalho-grupo {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 4px 10px;
  padding: 2px 4px 4px;
  border: 0;
  border-bottom: 1px solid #eef2f7;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.cabecalho-grupo:hover strong,
.cabecalho-grupo:hover .topico-aula {
  color: #1d4ed8;
}

.cabecalho-grupo:focus-visible {
  border-radius: 6px;
  outline: 2px solid rgb(37 99 235 / 55%);
  outline-offset: 2px;
}

.topico-aula {
  min-width: 0;
  color: #334155;
  font-size: 13px;
  font-weight: 500;
  overflow-wrap: anywhere;
}

.lista-resultados {
  display: grid;
  gap: 8px;
}

.resultado {
  display: grid;
  width: 100%;
  min-width: 0;
  gap: 5px;
  padding: 11px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color 140ms ease, background-color 140ms ease;
}

.resultado:hover {
  border-color: #cbd5e1;
  background: #fbfdff;
}

.linha-disciplina {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 7px;
  color: #334155;
  font-size: 13px;
}

.linha-disciplina strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.indicador-disciplina {
  width: 8px;
  height: 8px;
  flex: 0 0 8px;
  border-radius: 50%;
  background: var(--cor-disciplina);
}

.icone-disciplina {
  font-size: 14px;
  line-height: 1;
}

.data-horario {
  color: #64748b;
  font-size: 12px;
}

.titulo-anotacao {
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.trecho-anotacao {
  display: -webkit-box;
  overflow: hidden;
  color: #344054;
  font-size: 13px;
  line-height: 1.45;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.rodape-resultado {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 2px;
}

.tags-resultado,
.tags-resultado-vazias {
  flex: 1;
  min-width: 0;
}

.autor-anotacao {
  flex: none;
  color: #64748b;
  font-size: 12px;
}

.indicadores-midia,
.indicadores-midia > span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
}

.estado-pesquisa,
.estado-erro {
  padding: 18px 0;
  color: #64748b;
  font-size: 13px;
}

.estado-pesquisa strong,
.estado-pesquisa span {
  display: block;
}

.estado-pesquisa span {
  margin-top: 4px;
}

.estado-erro {
  color: #b91c1c;
}

.estado-disciplina {
  display: grid;
  justify-items: start;
  gap: 4px;
}

.estado-disciplina strong {
  color: #334155;
}

.estado-disciplina a {
  color: #2563eb;
  font-weight: 500;
}

.estado-disciplina .limpar-filtros {
  margin-top: 8px;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  clip-path: inset(50%);
}

@media (max-width: 600px) {
  .barra-filtros {
    align-items: stretch;
  }

  .filtro-wrapper {
    max-width: 100%;
  }

  .botao-filtro {
    max-width: min(100%, 240px);
  }

  .limpar-filtros {
    justify-content: center;
  }
}

@media (prefers-reduced-motion: reduce) {
  .resultado,
  .botao-filtro {
    transition: none;
  }
}
</style>