<script setup lang="ts">
import { Image as ImageIcon, PanelLeftClose, PanelLeftOpen, Plus } from '@lucide/vue'
import type { Anotacao } from '~/types/anotacao'

// Barra lateral da página da aula: um item grande por anotação. Com título, mostra o título e o
// começo do texto; sem título, só o texto, com mais linhas. Recolhida, vira uma faixa estreita
// com os botões de expandir e de nova anotação.

defineProps<{
  anotacoes: Anotacao[]
  // null quando o editor está com uma anotação nova, ainda não salva
  selecionadaId: string | null
  carregando: boolean
  recolhida: boolean
}>()

const emit = defineEmits<{
  selecionar: [anotacaoId: string]
  nova: []
  alternar: []
}>()

const { usuario } = useAuth()

const MAXIMO_TAGS_NO_ITEM = 3

function ehDoUsuario(anotacao: Anotacao) {
  return Boolean(usuario.value) && anotacao.autorId === usuario.value?.id
}
</script>

<template>
  <nav v-if="recolhida" class="lista-anotacoes recolhida" aria-label="Anotações da aula">
    <button
      type="button"
      class="botao-icone"
      aria-label="Expandir lista de anotações"
      title="Expandir lista de anotações"
      aria-expanded="false"
      @click="emit('alternar')"
    >
      <PanelLeftOpen :size="18" :stroke-width="1.8" aria-hidden="true" />
    </button>
    <span class="contagem" :title="`${anotacoes.length} anotação(ões)`">{{ anotacoes.length }}</span>
    <button
      type="button"
      class="botao-icone"
      aria-label="Nova anotação"
      title="Nova anotação"
      :aria-pressed="selecionadaId === null"
      @click="emit('nova')"
    >
      <Plus :size="18" :stroke-width="2" aria-hidden="true" />
    </button>
  </nav>

  <nav v-else class="lista-anotacoes" aria-label="Anotações da aula">
    <div class="topo-lista">
      <div class="linha-titulo">
        <h2>Anotações <span class="contagem">{{ anotacoes.length }}</span></h2>
        <button
          type="button"
          class="botao-icone"
          aria-label="Recolher lista de anotações"
          title="Recolher lista de anotações"
          aria-expanded="true"
          @click="emit('alternar')"
        >
          <PanelLeftClose :size="18" :stroke-width="1.8" aria-hidden="true" />
        </button>
      </div>
      <button
        type="button"
        class="botao-nova"
        :aria-pressed="selecionadaId === null"
        @click="emit('nova')"
      >
        <Plus :size="15" :stroke-width="2" aria-hidden="true" />
        Nova anotação
      </button>
    </div>

    <p v-if="carregando" class="estado-lista" role="status">Carregando anotações...</p>
    <p v-else-if="!anotacoes.length" class="estado-lista">Nenhuma anotação nesta aula ainda.</p>

    <ul v-else class="itens">
      <li v-for="anotacao in anotacoes" :key="anotacao.id">
        <button
          type="button"
          class="item"
          :class="{ selecionado: anotacao.id === selecionadaId }"
          :aria-current="anotacao.id === selecionadaId ? 'true' : undefined"
          @click="emit('selecionar', anotacao.id)"
        >
          <strong v-if="anotacao.titulo" class="titulo">{{ anotacao.titulo }}</strong>
          <span
            v-if="anotacao.conteudo"
            class="trecho"
            :class="{ 'trecho-sem-titulo': !anotacao.titulo }"
          >{{ anotacao.conteudo }}</span>

          <span v-if="anotacao.tags.length || anotacao.imagens.length || !ehDoUsuario(anotacao)" class="rodape-item">
            <span v-for="tag in anotacao.tags.slice(0, MAXIMO_TAGS_NO_ITEM)" :key="tag" class="tag">{{ tag }}</span>
            <span v-if="anotacao.tags.length > MAXIMO_TAGS_NO_ITEM" class="tag">
              +{{ anotacao.tags.length - MAXIMO_TAGS_NO_ITEM }}
            </span>
            <span v-if="anotacao.imagens.length" class="anexos" :title="`${anotacao.imagens.length} imagem(ns) anexada(s)`">
              <ImageIcon :size="13" :stroke-width="1.8" aria-hidden="true" />
              <span class="visually-hidden">Possui imagem anexada</span>
              {{ anotacao.imagens.length }}
            </span>
            <span v-if="!ehDoUsuario(anotacao)" class="autor">por {{ anotacao.autorNome }}</span>
          </span>
        </button>
      </li>
    </ul>
  </nav>
</template>

<style scoped>
.lista-anotacoes {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  border-right: 1px solid #e2e8f0;
  background: #f8fafc;
}

.topo-lista {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid #e2e8f0;
}

.linha-titulo {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.recolhida {
  align-items: center;
  gap: 8px;
  padding: 12px 0;
}

.botao-icone {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 7px;
  background: transparent;
  color: #475569;
  cursor: pointer;
}

.botao-icone:hover,
.botao-icone[aria-pressed='true'] {
  border-color: #e2e8f0;
  background: #fff;
  color: #1d4ed8;
}

h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.contagem {
  padding: 1px 7px;
  border-radius: 999px;
  background: #e2e8f0;
  color: #475569;
  font-size: 11px;
  letter-spacing: 0;
}

.botao-nova {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 36px;
  padding: 0 12px;
  border: 1px dashed #93c5fd;
  border-radius: 8px;
  background: #fff;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.botao-nova:hover,
.botao-nova[aria-pressed='true'] {
  border-style: solid;
  background: #eff6ff;
}

.estado-lista {
  margin: 0;
  padding: 16px 14px;
  color: #64748b;
  font-size: 13px;
}

.itens {
  flex: 1;
  min-height: 0;
  margin: 0;
  padding: 10px;
  overflow-y: auto;
  list-style: none;
  display: grid;
  align-content: start;
  gap: 8px;
}

.item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
  min-height: 104px;
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  color: #0f172a;
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color 120ms ease, box-shadow 120ms ease;
}

.item:hover {
  border-color: #bfdbfe;
}

.item.selecionado {
  border-color: #2563eb;
  box-shadow: 0 0 0 1px #2563eb;
  background: #f8fbff;
}

.titulo {
  overflow: hidden;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trecho {
  display: -webkit-box;
  overflow: hidden;
  color: #475569;
  font-size: 13px;
  line-height: 1.45;
  white-space: pre-line;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.trecho-sem-titulo {
  color: #1e293b;
  font-size: 14px;
  -webkit-line-clamp: 4;
}

.rodape-item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 5px;
  margin-top: auto;
  padding-top: 2px;
}

.tag {
  padding: 1px 7px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 11px;
  font-weight: 500;
}

.anexos {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: #64748b;
  font-size: 11px;
}

.autor {
  margin-left: auto;
  color: #64748b;
  font-size: 11px;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
}

.botao-nova:focus-visible,
.botao-icone:focus-visible,
.item:focus-visible {
  outline: 2px solid rgb(37 99 235 / 55%);
  outline-offset: 2px;
}

/* em telas estreitas a lista fica acima do editor, então a faixa recolhida é horizontal */
@media (max-width: 900px) {
  .lista-anotacoes {
    border-right: 0;
    border-bottom: 1px solid #e2e8f0;
  }

  .recolhida {
    flex-direction: row;
    padding: 6px 12px;
  }
}
</style>
