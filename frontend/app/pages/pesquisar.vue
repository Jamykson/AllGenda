<script setup lang="ts">
import { BookOpen, CalendarDays, ChevronDown, Clock3, Image as ImageIcon, Search, Tag as TagIcon, Video as VideoIcon, X } from '@lucide/vue'
import type { AulaPesquisa } from '~/composables/usePesquisa'

definePageMeta({
  layout: 'default'
})

const {
  disciplinas,
  tagsDisponiveis,
  carregando,
  erro,
  resultados,
  carregar
} = usePesquisa()

const textoBusca = ref('')
const disciplinaId = ref('')
const dataInicial = ref('')
const dataFinal = ref('')
const horaInicial = ref('')
const horaFinal = ref('')
const tagsSelecionadas = ref<string[]>([])
const buscaTag = ref('')
const aulaSelecionada = ref<AulaPesquisa | null>(null)
const barraFiltrosRef = ref<HTMLElement | null>(null)
const filtroAberto = ref<'disciplina' | 'data' | 'horario' | 'tags' | null>(null)

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
      if (consulta && !normalizarTexto(anotacao.texto).includes(consulta)) {
        return false
      }

      if (disciplinaId.value && aula.disciplinaId !== disciplinaId.value) {
        return false
      }

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

const filtrosAtivos = computed(() => Boolean(
  textoBusca.value.trim()
  || disciplinaId.value
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

function abrirAula(aula: AulaPesquisa) {
  aulaSelecionada.value = aula
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
          <span>{{ disciplinas.find(item => item.id === disciplinaId)?.nome || 'Todas as disciplinas' }}</span>
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
        <span v-if="!carregando && !erro">{{ resultadosFiltrados.length }}</span>
      </header>

      <p v-if="carregando" class="estado-pesquisa" role="status">
        Carregando dados para pesquisa...
      </p>

      <p v-else-if="erro" class="estado-erro" role="alert">
        {{ erro }}
      </p>

      <p v-else-if="!resultadosFiltrados.length && filtrosAtivos" class="estado-pesquisa">
        Nenhum resultado encontrado. Tente remover alguns filtros ou pesquisar outro termo.
      </p>

      <p v-else-if="!resultadosFiltrados.length" class="estado-pesquisa">
        <strong>Nenhuma anotação encontrada.</strong>
        <span>Suas anotações aparecerão aqui quando forem adicionadas às aulas.</span>
      </p>

      <div v-else class="lista-resultados">
        <button
          v-for="resultado in resultadosFiltrados"
          :key="`${resultado.aula.id}-${resultado.anotacao.id}`"
          type="button"
          class="resultado"
          :aria-label="`Abrir aula de ${resultado.aula.disciplinaNome}, ${formatarData(resultado.aula.data)}`"
          @click="abrirAula(resultado.aula)"
        >
          <span class="linha-disciplina">
            <span
              class="indicador-disciplina"
              :style="{ '--cor-disciplina': resultado.aula.disciplinaCor || '#94a3b8' }"
              aria-hidden="true"
            />
            <span v-if="resultado.aula.disciplinaIcone" class="icone-disciplina" aria-hidden="true">
              {{ resultado.aula.disciplinaIcone }}
            </span>
            <strong>{{ resultado.aula.disciplinaNome }}</strong>
          </span>

          <span class="data-horario">
            {{ formatarData(resultado.aula.data) }} · {{ resultado.aula.horario }}
          </span>

          <span v-if="resultado.anotacao.texto.trim()" class="trecho-anotacao">
            {{ resultado.anotacao.texto }}
          </span>

          <span class="rodape-resultado">
            <span v-if="resultado.anotacao.tags.length" class="tags-resultado">
              <span v-for="tag in resultado.anotacao.tags" :key="tag" class="chip-tag">
                {{ tag }}
              </span>
            </span>
            <span v-else class="tags-resultado-vazias" />

            <span v-if="resultado.anotacao.imagens.length || resultado.anotacao.videos.length" class="indicadores-midia">
              <span v-if="resultado.anotacao.imagens.length" :title="`${resultado.anotacao.imagens.length} imagem(ns)`">
                <ImageIcon :size="14" :stroke-width="1.8" aria-hidden="true" />
                <span class="visually-hidden">Possui imagem</span>
              </span>
              <span v-if="resultado.anotacao.videos.length" :title="`${resultado.anotacao.videos.length} vídeo(s)`">
                <VideoIcon :size="14" :stroke-width="1.8" aria-hidden="true" />
                <span class="visually-hidden">Possui vídeo</span>
              </span>
            </span>
          </span>
        </button>
      </div>
    </section>

    <AulaModal
      v-if="aulaSelecionada"
      :aula="aulaSelecionada"
      @fechar="aulaSelecionada = null"
    />
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

.opcao-disciplina:hover,
.opcao-disciplina.ativa {
  background: #eff6ff;
  color: #1d4ed8;
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

.tags-resultado-vazias {
  flex: 1;
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